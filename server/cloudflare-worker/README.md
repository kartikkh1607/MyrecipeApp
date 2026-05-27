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
- **Premium gate:** `POST /gemini` requests with `generationConfig.responseMimeType =
  "application/json"` (recipe generation) require the `premium` custom claim, else `402
  {"error":"premium_required"}`. Plain chat is unaffected. The claim is set by
  `/billing/verify` after Google Play validates the subscription.

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
- Response caching (the cross-user "first user pays" cache) can be added here later
  with Workers KV — deferred for now to keep v1 simple.
