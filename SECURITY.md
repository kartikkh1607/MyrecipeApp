# MealTime — Security Model

This document describes what the app protects against, what it does **not** protect against, and what you need to configure outside the codebase.

---

## What's Protected

### Network
- **HTTPS-only enforced** via `res/xml/network_security_config.xml`. Cleartext HTTP traffic is blocked on Android 7+.
- **System CA store only** in release. User-installed certs (Charles Proxy, etc.) only trusted in debug builds.

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

---

## What's NOT Protected

Be honest about limits.

### API Keys in the APK
**Spoonacular and Gemini API keys live in `BuildConfig`.** R8 obfuscation makes them mildly harder to find by string-scanning the APK, but a determined attacker WILL extract them via tools like `apktool` or `jadx`.

**Mitigations available:**
1. **Spoonacular**: set per-key daily/monthly quotas in your Spoonacular dashboard. If a key gets abused, it stops working before your bill explodes.
2. **Gemini**: enable [API key restrictions](https://console.cloud.google.com/apis/credentials) — restrict to Android app + your app's SHA-1 fingerprint. Abusers can't use the key from elsewhere.
3. **Long-term**: move all API calls to a Firebase Cloud Function. App authenticates to YOUR function with a Firebase Auth token; YOUR function calls Spoonacular/Gemini with the real key. The key never leaves the server. See "Cloud Function Proxy" below.

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

### 4. AdMob — Replace Test IDs
Before Play Store upload:
- `app/src/main/java/com/example/myrecipeapp/data/ads/AdConfig.kt` → set `PROD_BANNER_ID` and `PROD_INTERSTITIAL_ID`
- `AndroidManifest.xml` → replace the test `APPLICATION_ID` meta-data value

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

### Cloud Function Proxy (eliminates API key leak risk)
1. Upgrade Firebase to Blaze plan (pay-as-you-go, free tier covers small apps)
2. Create `functions/src/index.ts` with two callable functions: `spoonacularSearch`, `geminiChat`
3. Each function reads the API key from `functions.config()` (server-side env vars)
4. Each function verifies `context.auth` (Firebase Auth token) and rate-limits per `uid`
5. Update `SpoonacularApiService` and `GeminiAiService` to call your Cloud Functions instead of the third-party APIs directly
6. Rotate the leaked Spoonacular/Gemini keys in their dashboards

Estimated work: 1-2 days. Estimated cost: free for <2M function invocations/month.

### Play Integrity API
1. Enable Play Integrity in Play Console
2. On sensitive actions (subscription verification, premium feature gates), request an integrity verdict
3. Reject requests where verdict says the app or device is compromised

### Certificate Pinning
Pin both `api.spoonacular.com` and `generativelanguage.googleapis.com` in OkHttp's `CertificatePinner`. Get pins from `openssl s_client -connect host:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform DER | openssl dgst -sha256 -binary | base64`. **Warning**: when certs rotate, the app breaks for everyone until you ship an update.

### Encrypted Database
The Room DB stores favorites + shopping list — not particularly sensitive. If you start storing meal plans or other personal data, swap Room's default SQLite for [SQLCipher](https://github.com/sqlcipher/android-database-sqlcipher) with a key derived from Android Keystore.
