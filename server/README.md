# MealTime API Proxy

Keeps the **Spoonacular**, **Gemini**, and **Groq** API keys server-side instead of
shipping them inside the Android APK. This is the proper fix for the key-exposure
risk noted in `app/proguard-rules.pro` ("This is not real security — a determined
attacker WILL extract the keys").

This is a **scaffold**: it is complete server code, but you must deploy it (needs a
Firebase project on the **Blaze** pay-as-you-go plan, because Cloud Functions can only
make outbound calls to third-party APIs on Blaze) and then point the app at it.

## What it does

Three authenticated Cloud Functions (2nd gen, Node 20) that forward requests upstream
with the real key injected. Every call requires a valid **Firebase Auth ID token**, so
the proxy is not an open relay.

| Function      | App calls                                   | Forwards to |
| ------------- | ------------------------------------------- | ----------- |
| `spoonacular` | `GET /spoonacular/<path>?<query>`           | `api.spoonacular.com/<path>` + `apiKey` |
| `gemini`      | `POST /gemini` (GenerateContentRequest)     | Gemini `:generateContent` + `?key=` |
| `groq`        | `POST /groq` (`{messages, temperature,...}`)| Groq `chat/completions` + `Bearer` |

## Shared response cache (cost saver)

The `spoonacular` and `gemini` functions cache upstream responses in a Firestore
collection (`apiCache`), keyed by a hash of the normalised request. The **first**
signed-in user to make a given request pays the API call; **every** user after
that is served from Firestore until the entry expires. Responses carry an
`X-Cache: HIT | MISS | SKIP` header so you can confirm it's working.

| Function | Cached? | Key | TTL |
| -------- | ------- | --- | --- |
| `spoonacular` | Yes (all GETs) | normalised path + sorted query | 7 days |
| `gemini` | Only structured-output calls (`responseMimeType=application/json`, i.e. recipe generation/transform/meal plan) | hash of request body | 30 days |
| `gemini` chat / recommendations | No (personalised/conversational) | — | — |
| `groq` | No (fallback only) | — | — |

Why this matters: Spoonacular's free tier is a **hard** daily point quota, and a
recipe app's queries are highly repetitive — cache hit rates of 80–95% are
realistic, so duplicate searches stop costing quota entirely. A Firestore read
(~$0.06 / 100k, 50k/day free) is far cheaper than an upstream call. Only `200`s
are cached, so a transient `429`/`500` is never served back for days.

### One-time setup you must do: enable the Firestore TTL policy

Without this, expired cache docs are still honoured (the code checks `expireAt`)
but they never get deleted, so the collection grows forever. To auto-delete:

```bash
gcloud firestore fields ttls update expireAt \
  --collection-group=apiCache \
  --enable-ttl \
  --project=<your-project-id>
```

Or in the console: **Firestore Database → TTL → Create policy**, collection
group `apiCache`, timestamp field `expireAt`.

> Client access to `apiCache` is already blocked by the default-deny rule in
> `firestore.rules`; Cloud Functions use the Admin SDK and bypass rules, so the
> cache keeps working.

## Deploy

```bash
cd server
firebase init functions      # if not already wired; pick the existing MealTime project
cd functions && npm install

# Store the keys as secrets (NOT in source / not in local.properties):
firebase functions:secrets:set SPOONACULAR_API_KEY
firebase functions:secrets:set GEMINI_API_KEY
firebase functions:secrets:set GROQ_API_KEY

firebase deploy --only functions
```

Deploy prints the function URLs, e.g.
`https://us-central1-<project>.cloudfunctions.net/spoonacular`.

## Client changes (in the Android app, after deploy)

1. **Remove the keys from the client**: delete the `buildConfigField` lines for
   `SPOONACULAR_API_KEY`, `GEMINI_API_KEY`, `GROQ_API_KEY` in `app/build.gradle.kts`
   and the matching entries in `local.properties`.
2. **Spoonacular** (`data/remote/NetworkModule.kt`): change `BASE_URL` to the
   `spoonacular` function URL and **delete `ApiKeyInterceptor`** (the server adds the key).
3. **Auth header**: add an interceptor that attaches the current user's Firebase ID
   token — `FirebaseAuth.getInstance().currentUser?.getIdToken(false)` — as
   `Authorization: Bearer <token>` on every proxied request.
4. **Gemini / Groq** (`GeminiAiService.kt`, `GroqAiService.kt`): point `BASE_URL` at the
   `gemini` / `groq` function URLs and drop the `?key=` / `Bearer <key>` lines — the
   server supplies them. Keep sending the same JSON bodies.
5. Rotate the old keys in the Spoonacular / Google AI / Groq consoles, since the
   currently-published APK still contains them.

## Play Billing (premium subscription)

The premium AI features are sold as a Play **subscription** (`premium`, base plans
`monthly` + `annual`). Verification and entitlement do **not** live here — they run on
the **Cloudflare Worker** (`server/cloudflare-worker`), because Cloud Functions need the
Blaze plan and this project stays card-free. The Worker mints a Google service-account
token to validate the purchase with the Play Developer API and sets the `premium` Firebase
**custom claim**; the `gemini` proxy here (and the Worker) reject structured
recipe-generation (`responseMimeType=application/json`) with `402 premium_required` unless
the verified token carries `premium: true`. Chat stays free.

➡️ Full setup (subscription product, service-account, KV, RTDN, license testers) is in
**`server/cloudflare-worker/README.md` → "Play Billing"**.

## Notes

- `GEMINI_MODEL` / `GROQ_MODEL` are centralised here — change the model in one place
  without shipping an app update.
- Consider adding [Firebase App Check](https://firebase.google.com/docs/app-check) on
  top of the auth check to further restrict callers to your genuine app build.
- Add per-user rate limiting (e.g. Firestore counters) if quota abuse by signed-in
  users becomes a concern.
