# MealTime API Proxy — Cloudflare Worker

Keeps the **Spoonacular**, **Gemini**, and **Groq** API keys off the Android APK
by holding them server-side. The app calls this Worker; the Worker injects the
key and forwards the request. Free Cloudflare plan, **no credit card** (100,000
requests/day).

Every request must carry a valid **Firebase ID token** for the MealTime project
(`Authorization: Bearer <token>`), so the Worker is not an open relay.

```
App ──(Firebase ID token)──► Worker (holds keys) ──► Spoonacular / Gemini / Groq
```

| Route                              | Forwards to |
| ---------------------------------- | ----------- |
| `GET  /spoonacular/<path>?<query>` | `api.spoonacular.com/<path>` + `apiKey` |
| `POST /gemini`                     | Gemini `:generateContent` + `?key=` |
| `POST /groq`                       | Groq `chat/completions` + `Bearer` |
| `POST /billing/verify`             | Play Developer API → sets `premium` claim (see below) |
| `POST /billing/rtdn?key=…`         | Pub/Sub push for subscription renewals/cancels (no Firebase token) |

## Deploy (run these yourself)

All commands run **from this folder**: `server/cloudflare-worker`.

```powershell
# 1. Log in (opens a browser to authorize). One time.
wrangler login

# 2. Store the three keys as secrets. Each prompts you to paste the key.
wrangler secret put SPOONACULAR_API_KEY
wrangler secret put GEMINI_API_KEY
wrangler secret put GROQ_API_KEY

# 3. Deploy.
wrangler deploy
```

`wrangler deploy` prints the URL, e.g.
`https://mealtime-proxy.<your-subdomain>.workers.dev`. **Send that URL back** so
the app can be pointed at it.

## Notes

- `FIREBASE_PROJECT_ID` in `wrangler.toml` (`mealtime-96386`) is public — it only
  identifies which project's login tokens to accept. Not a secret.
- `GEMINI_MODEL` / `GROQ_MODEL` are set in `src/index.js` — change the model in one
  place without shipping an app update.
- Test a route after deploy (replace `<URL>` and `<TOKEN>` with a real ID token):
  ```powershell
  curl.exe -H "Authorization: Bearer <TOKEN>" "<URL>/spoonacular/recipes/complexSearch?query=pasta&number=1"
  ```
  Without a token it must return `401`.
- This replaces the Firebase Cloud Functions version in `server/functions/`, which needed
  the paid Blaze plan. Everything — proxy **and** billing — now runs here, card-free.
- **Premium gate:** Structured AI generation requires the `premium` custom claim, else
  `402 {"error":"premium_required"}`. Plain chat is unaffected. The claim is set by
  `/billing/verify` after Google Play validates the subscription. Both upstreams are
  gated server-side:
  - `POST /gemini` — gated when `generationConfig.responseMimeType = "application/json"`.
  - `POST /groq` — gated when `response_format.type` is `"json_object"` or `"json_schema"`.
    This mirrors `/gemini` so the router's Groq-fallback path on Gemini failures can't
    bypass the paywall via a crafted direct call.
- **Per-user daily quota (Gemini):** `POST /gemini` is capped per uid per UTC-day to
  protect the paid upstream from a runaway user. Defaults (set in `wrangler.toml
  [vars]`): **25/day free, 200/day premium**. When exceeded the Worker returns `429
  {"error": "user_quota_exceeded", "limit": N, "tier": "free|premium"}`. Counters live
  in `BILLING_KV` (`quota:gemini:<uid>:<YYYY-MM-DD>`) and auto-expire 60 h after write.
- **Per-uid rate limit (all routes):** Every authenticated route is rate-limited per uid
  per UTC-minute. Default **60/min** (set `RATE_LIMIT_PER_MINUTE` in `wrangler.toml`).
  Exceeded → `429 {"error":"rate_limit_exceeded","limit":N,"used":M}` with a
  `Retry-After` header. Counter lives in `BILLING_KV` (`rl:<uid>:<minute>`), TTL 120 s.
  Fails open if KV is unavailable so a KV blip doesn't lock everyone out.
- **Shared response cache (first user pays):**
  - `GET /spoonacular/*` (2xx only) → Cloudflare **Cache API**, 7-day TTL. Key is the
    normalised path + query (sort + trim + lowercase, `apiKey` excluded), so "Pasta " and
    "pasta" collapse to one entry. Per-edge on `workers.dev`, zone-wide on a custom domain.
  - `POST /gemini` structured-output calls (2xx only) → **KV** (`cache:gemini:<sha256>`),
    30-day TTL. Cross-edge global hits because the same prompt + dietary prefs across
    users yields the same recipe/meal-plan output. Cache hit short-circuits BEFORE the
    daily quota increment — a free cached response doesn't burn a slot.
  - Chat/recommendations are personalised and conversational — never cached. Responses
    carry `X-Cache: HIT | MISS | SKIP` so you can grep the worker tail for hit rate.
  Change any of the limits/TTLs in `wrangler.toml` (vars) or `src/index.js` (TTL consts)
  and `wrangler deploy` — no app update needed.

## Cost guardrails

The per-user quota above caps Gemini abuse on the **app** side. You should also set a
hard ceiling on the **GCP project** side so a billing/upstream bug can't drain the card.

1. **Set a hard GCP budget alert** — *GCP Console → Billing → Budgets & alerts → Create
   budget*. Scope to the Gemini-billing project, set a monthly cap (e.g. **$10**), and
   alert at 50 / 90 / 100 %. (GCP doesn't auto-stop on budget — pair with a Cloud
   Function that disables the API on the 100 % alert if you want a true kill switch.)
2. **Cap the Gemini API quota** in *GCP Console → APIs & Services → Generative Language
   API → Quotas* so the daily request count can't exceed your expected ceiling even if
   a code bug ignores the per-user cap.
3. **Watch Worker logs** with `wrangler tail` during launch week — every quota rejection
   logs nothing by default; if you want to track them, add a `console.log` in
   `checkAndIncrementGeminiQuota` before the `return` for refused requests.

## Play Billing (premium subscription)

Premium AI is a Play **subscription** (`premium`, base plans `monthly` + `annual`).
`/billing/verify` validates a purchase with the Play Developer API and sets the `premium`
Firebase **custom claim**; `/billing/rtdn` keeps it current on renewals/cancels. No Cloud
Functions / Blaze — the Worker mints a Google service-account token (RS256 JWT) and calls
the Play + Identity Toolkit REST APIs directly. A purchaseToken→uid map lives in KV.

### One-time setup (you)

1. **Subscription:** Play Console → create product `premium` with base plans `monthly` and
   `annual` (auto-renewing); activate them.
2. **Service account:** Firebase console → Project Settings → Service Accounts → *Generate
   new private key*. From that JSON, set two secrets (the SA also needs the **Firebase
   Authentication Admin** IAM role — the default `firebase-adminsdk` SA already has it):
   ```powershell
   wrangler secret put GCP_SA_CLIENT_EMAIL   # the JSON's client_email
   wrangler secret put GCP_SA_PRIVATE_KEY    # the JSON's private_key (full PEM)
   ```
3. **Play Developer API access:** enable the Android Publisher API in the GCP project, and
   in Play Console → *Setup → API access* grant that service account "view financial data /
   manage orders".
4. **KV namespace:** `wrangler kv namespace create BILLING_KV`, then paste the printed id
   into `wrangler.toml` (`[[kv_namespaces]]`).
5. **RTDN:** create a Pub/Sub topic, set it in Play Console → *Monetization setup*, and add
   a **push** subscription to `https://<worker-url>/billing/rtdn?key=<RTDN_SECRET>`. Set the
   secret: `wrangler secret put RTDN_SECRET`.
6. **License testers:** Play Console → *Setup → License testing* — add your test account so
   you can subscribe without being charged. Test on the **Internal testing** track; Billing
   does not work on a plain local/debug install.

After setup: `wrangler deploy`.
