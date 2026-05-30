# MealTime — Security Model

This document describes what the app protects against, what it does **not** protect against, and what you need to configure outside the codebase.

---

## What's Protected

### Network
- **HTTPS-only enforced** via `res/xml/network_security_config.xml`. Cleartext HTTP traffic is blocked on Android 7+.
- **System CA store only** in release. User-installed certs (Charles Proxy, etc.) only trusted in debug builds.
- **No third-party API keys in the APK.** Spoonacular / Gemini / Groq calls are all routed through a **Cloudflare Worker** that holds the keys server-side. The APK only knows the Worker's URL.
- **Every Worker call is Firebase-authenticated.** `FirebaseAuthInterceptor` attaches the current user's Firebase ID token; the Worker validates it (RS256, against Google's JWK set) and rejects un-authenticated traffic with `401`. The Worker is not an open relay.
- **Per-uid rate-limit + quota** enforced on the Worker (`RATE_LIMIT_PER_MINUTE`, plus per-day Gemini quota that distinguishes free vs `premium` tier) so a single bad actor with a token can't drain shared API quota for everyone.
- **Premium gating is server-side.** Structured-output Gemini and Groq calls (recipe generation, transform, meal plan) require the `premium` Firebase custom claim — the Worker returns `402 premium_required` otherwise, and the client surfaces the upsell sheet. The Groq path enforces the same gate so the AI failover can't be used as a paywall bypass.

### Code
- **R8/ProGuard obfuscation** enabled in release (`isMinifyEnabled = true`, `isShrinkResources = true`).
- **All log calls stripped** in release (`Log.v/d/i/w/e/wtf`, `Throwable.printStackTrace`, `System.out.println`). No user actions, API behaviour, or stack traces leak to logcat.
- **Source-file attribute renamed** so decompiled stack traces don't include original `.kt` filenames.
- **Classes repackaged** into a single obfuscated namespace to make static analysis harder.

### User Data
- **`android:allowBackup="false"`** in manifest. The OS won't back up app data to Google Drive.
- **`backup_rules.xml` + `data_extraction_rules.xml`** explicitly exclude sharedprefs, databases, and file storage from cloud backup AND device-to-device transfer (Android 12+).
- **`FLAG_SECURE` on AuthScreen** — passwords cannot be screenshotted, recorded, or shown in the app-switcher thumbnail.

### Auth
- **Firebase Authentication** handles all credential storage. We never see the raw password — it goes from `BasicTextField` straight to `FirebaseAuth.signIn`.
- **Email format validation** rejects malformed emails before any network call.
- **Password minimum** 6 chars (Firebase Auth default).
- **Account state observed via `StateFlow`** — UI reactively logs out if the auth token is revoked.

### Permissions
| Permission | Why | Risk if removed |
|------------|-----|-----------------|
| `INTERNET` | All API calls | App won't work |
| `RECORD_AUDIO` | Cooking Mode voice commands | Voice control disabled |
| `AD_ID` (Google Play Services) | AdMob ad personalisation | Ads still show, just less relevant |

No `READ_EXTERNAL_STORAGE`, `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, or other high-risk permissions.

### Observability of background failures
- Firestore sync is best-effort by design (a bad network shouldn't crash the app), but silent failures used to mean a quietly broken sync looked identical to a healthy one. The `syncBestEffort` helper in `data/repository/SyncRepository.kt` records every failure as a **Crashlytics non-fatal** with the operation name (`uploadFavorite`, `clearShoppingList`, etc.) — so we get visibility without changing user-facing behaviour.
- Coroutine cancellation propagates to in-flight Gemini requests via `suspendCancellableCoroutine`, so a user navigating away from a long generation stops paying for it immediately instead of letting the request finish on its own.

### Build verification
Every push and PR runs `./gradlew :app:assembleDebug :app:lintDebug :app:testDebugUnitTest` via [GitHub Actions](./.github/workflows/ci.yml). Lint *errors* fail the build, so issues like `NonObservableLocale` in composables can't sneak back in.

---

## What's NOT Protected

Be honest about limits.

### Rooted / Modified Devices
We don't detect root, custom ROMs, or modified APKs. Standard hardening only. A motivated user CAN:
- Decompile the APK
- Patch out the ad code
- Patch in fake premium state (once subscription ships)

For a recipe app this is acceptable. If you ever ship paid digital content, integrate [Play Integrity API](https://developer.android.com/google/play/integrity).

### MITM via custom root CA
We don't pin certificates. If a user installs a malicious root CA AND our trust-anchor was set to include user CAs, traffic could be intercepted. **Mitigation in place**: release builds only trust the system CA store (debug builds allow user CAs for proxy debugging).

Certificate pinning is intentionally skipped — it would break the app silently if Spoonacular or Gemini rotates their certs.

---

## What You Must Configure (Outside Code)

### 1. Firestore Security Rules — CRITICAL
Open Firebase Console → Firestore Database → Rules tab. Paste the contents of `firestore.rules` (in this repo) and click **Publish**.

Without these rules, ANY authenticated user could read and write ALL other users' favorites and shopping lists. The default Firebase rules are "deny all" on new projects but "allow all" on legacy ones — **verify**.

Test by opening the Rules Playground and trying to `get /users/SOMEONE_ELSES_UID/favorites/...` while authenticated as a different user. It must deny.

### 2. Gemini API Key Restriction
1. Go to https://console.cloud.google.com/apis/credentials
2. Find your Gemini key → "Restrict key"
3. Application restrictions: **Android apps**
4. Add package name `com.kartik.mealtime` + your release SHA-1 fingerprint (run `keytool -list -v -keystore release.keystore` to get it)

### 3. Spoonacular Quota
Log into Spoonacular dashboard → set daily quota at a level you can afford if the key leaks. ~150 requests/day is reasonable for a free tier launch.

### 4. AdMob — Configure Production IDs
Before Play Store upload, add your production AdMob IDs to `local.properties`
(this file is git-ignored, so the IDs never enter source control):

```properties
admob.app.id=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.banner.id=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
admob.interstitial.id=ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY
```

The build injects these via `BuildConfig` (ad-unit IDs) and the
`${admobAppId}` manifest placeholder (App ID) — see `app/build.gradle.kts`.
Debug builds always use Google's test IDs. **If the production unit IDs are
missing, release builds disable ads entirely** rather than serve test IDs as
production (`AdConfig.adsEnabled`), so a misconfigured release can never ship
test ads. Do not hardcode production IDs in `AdConfig.kt`.

### 5. Privacy Policy — REQUIRED
You must host a Privacy Policy and link it in the Play Console. It must disclose:
- Email collection (Firebase Auth)
- Personal data sync (Firestore)
- Crash reports (Crashlytics)
- Analytics (Firebase Analytics)
- Advertising ID usage (AdMob)
- Voice recordings (Cooking Mode — note these are processed on-device by Android Speech, not sent anywhere)

Free generator: https://app.freeprivacypolicy.com/

### 6. Play Console Data Safety Form
In the Play Console, fill in the "Data Safety" section honestly:
- Email address → collected, used for account management, encrypted in transit, can be deleted on request
- App activity (favorites, shopping list) → collected, used for app functionality
- Advertising ID → collected for ads
- Crash logs → collected for app performance

---

## Future Hardening (Optional, Post-Launch)

If the app gets traction and faces real attacks:

> The original "Cloud Function Proxy" item from this section has been **shipped** — see [`server/cloudflare-worker/`](./server/cloudflare-worker/). All third-party API calls now go through the Worker, which holds the keys, validates Firebase ID tokens, enforces per-uid rate limits + daily quotas, and gates premium AI features server-side. We're on Cloudflare's free plan rather than Firebase Blaze, so no credit card is required.

### Play Integrity API
1. Enable Play Integrity in Play Console
2. On sensitive actions (subscription verification, premium feature gates), request an integrity verdict
3. Reject requests where verdict says the app or device is compromised

### Certificate Pinning
Pin both `api.spoonacular.com` and `generativelanguage.googleapis.com` in OkHttp's `CertificatePinner`. Get pins from `openssl s_client -connect host:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform DER | openssl dgst -sha256 -binary | base64`. **Warning**: when certs rotate, the app breaks for everyone until you ship an update.

### Encrypted Database
The Room DB stores favorites + shopping list — not particularly sensitive. If you start storing meal plans or other personal data, swap Room's default SQLite for [SQLCipher](https://github.com/sqlcipher/android-database-sqlcipher) with a key derived from Android Keystore.
