# API Setup Guide

> **This document is now historical.** As of the Play Billing / Worker refactor (commit `99c42a4`), the app does **not** hold any third-party API keys. All Spoonacular, Gemini, and Groq traffic is proxied through a **Cloudflare Worker** that holds the keys server-side.
>
> If you are setting the project up for the first time, follow **[`server/cloudflare-worker/README.md`](./server/cloudflare-worker/README.md)** — it is the only place that needs API keys, and the setup is `wrangler login` → three `wrangler secret put` calls → `wrangler deploy`.

---

## What the app needs in `local.properties`

Only the Worker URL (and AdMob IDs for signed release builds). API keys are not part of this file.

```properties
# Cloudflare Worker proxy URL (override for dev / staging)
proxy.base.url=https://mealtime-proxy.<your-subdomain>.workers.dev

# AdMob (release only — debug uses Google's baked-in test IDs)
admob.app.id=ca-app-pub-XXXX~XXXX
admob.banner.id=ca-app-pub-XXXX/XXXX
admob.interstitial.id=ca-app-pub-XXXX/XXXX
```

`local.properties` is gitignored. If `admob.*` values are blank, `AdConfig.adsEnabled` returns `false` for release builds so test IDs can never ship as production.

---

## What the Worker forwards

| Route | Forwards to | Notes |
|---|---|---|
| `GET  /spoonacular/<path>` | `api.spoonacular.com/<path>` | Worker appends `apiKey` |
| `POST /gemini` | Gemini `:generateContent` | JSON-mode (structured AI features) requires the `premium` custom claim |
| `POST /groq` | Groq `chat/completions` | Free chat / recommendations fallback; structured calls (`generateRecipe`, `transformRecipe`, `generateMealPlan`) never fall back to Groq on a 402 to prevent paywall bypass |
| `POST /billing/verify` | Play Developer API | Validates a purchase and mints the `premium` custom claim |
| `POST /billing/rtdn` | (Pub/Sub push) | Real-time Developer Notifications keep the claim current on renewal / cancellation |

The Cloudflare free plan (no credit card, 100k requests/day) is enough for development and small-scale launches.

---

## Auth requirement

Every Worker call carries a Firebase ID token attached by `FirebaseAuthInterceptor`. The Worker rejects un-authenticated traffic with `401`. This is why the app dropped guest mode (commit `4718355`) — there is no graceful fallback for an un-signed-in user.

---

## Offline fallback

Even when the Worker is unreachable, the app does not crash:

1. Repositories first try the Worker route.
2. On failure, they read from the local **Room** cache (`cached_recipes`, `favorites`, `shopping_items`).
3. If the cache is empty, they read from `SampleDataSource` — a small bundled set of recipes that ships with the APK.

Search and AI features degrade to an error state (with a friendly retry button); browse / favorites / shopping list remain fully usable.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `401` on every Worker call | Firebase ID token missing — user not signed in, or token expired before refresh | Sign in again; check `FirebaseAuthInterceptor` is registered |
| `402 premium_required` from `/gemini` | Free user hit JSON-mode (structured AI) feature | Expected — the UI surfaces this as the upsell sheet |
| `429` from `/gemini` or `/groq` | Worker rate limit (per-IP / per-user) | Wait or upgrade the user to premium |
| Recipe browse shows only the same handful of items | Worker URL wrong / unreachable; falling back to `SampleDataSource` | Verify `proxy.base.url` in `local.properties`, then redeploy with `wrangler deploy` |

For Worker-side logs, run `wrangler tail` while reproducing the issue.
