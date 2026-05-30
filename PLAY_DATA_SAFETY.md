# Play Console — Data Safety form answers (MealTime)

**Purpose:** fill-in guide for the Play Console **Data safety** section
(Policy → App content → Data safety). This is derived from what the app
*actually* does — verified against `AndroidManifest.xml`, `app/build.gradle.kts`,
and the source — and is consistent with `docs/privacy-policy.md`.

> ⚠️ You are responsible for the final declaration. Re-check it whenever you
> add an SDK or a feature that sends data off the device. Google audits these.

**Last reviewed:** May 30, 2026 · app versionCode 2 / versionName 1.0.1

---

## SDKs / services that drive these answers
| Source | Pulls in | Relevant data |
|---|---|---|
| `play-services-ads` (AdMob) | Google Mobile Ads SDK | **Advertising ID**, ad-interaction + device info |
| `firebase-analytics` | Analytics | Usage/diagnostic events, app-instance ID |
| `firebase-crashlytics` | Crashlytics (release builds only) | Crash logs, device model, OS, stack traces |
| `firebase-auth` | Firebase Auth | Email, User ID (UID) — sign-in is mandatory; no guest mode |
| `firebase-firestore` | Cloud Firestore | Favorites, AI recipes, shopping list, preferences |
| Spoonacular API | Retrofit call | Search query text (transient) |
| Gemini API + Groq (fallback) | OkHttp call | AI prompt text (transient) |
| `android.speech.SpeechRecognizer` | OS speech (Cooking Mode) | Microphone audio — **not collected/stored by the app** |

Manifest permissions: `INTERNET`, `RECORD_AUDIO`, `com.google.android.gms.permission.AD_ID`.

---

## Part 1 — Overview questions
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** — `network_security_config` blocks cleartext; all calls are HTTPS |
| Do you provide a way for users to request that their data is deleted? | **Yes** — in-app **Profile → Delete account**, plus email request (see Privacy Policy §4) |

---

## Part 2 — Data types

Legend — for each type Google asks: **Collected?** / **Shared?** /
**Processed ephemerally?** / **Required or optional?** / **Purposes**.
"Shared" = sent to a third party using it for *their own* purposes. Google
acting as a **service provider/processor** (Firebase, Crashlytics, Analytics)
counts as *collected* but **not shared**. AdMob personalized ads **is** sharing.

### ✅ Declare these as COLLECTED

**Personal info → Email address**
- Collected: **Yes** · Shared: **No** · Ephemeral: No · **Required** (sign-in is mandatory — guest mode was removed in commit `4718355`)
- Purposes: **Account management**, **App functionality**

**Personal info → User IDs** (Firebase Auth UID)
- Collected: **Yes** · Shared: **No** · Ephemeral: No · **Required** (sign-in is mandatory)
- Purposes: **Account management**, **App functionality**

**App activity → Other user-generated content** (favorites, AI-generated recipes, shopping-list items, dietary/theme preferences synced to Firestore)
- Collected: **Yes** · Shared: **No** · Ephemeral: No · **Required** (sign-in is mandatory; sync is the default mode)
- Purposes: **App functionality**

**Device or other IDs → Device or other IDs** (Advertising ID via AdMob; Firebase installation / Analytics app-instance ID)
- Collected: **Yes** · Shared: **Yes** (Advertising ID → Google for ads) · Ephemeral: No · **Required** (in the free, ad-supported version)
- Purposes: **Advertising or marketing**, **Analytics**, **Fraud prevention and security**

**App info and performance → Crash logs** (Crashlytics, release builds)
- Collected: **Yes** · Shared: **No** · Ephemeral: No · **Required** (automatic)
- Purposes: **App functionality**, **Analytics**

**App info and performance → Diagnostics** (Firebase Analytics usage/perf events)
- Collected: **Yes** · Shared: **No** · Ephemeral: No · **Required** (automatic)
- Purposes: **Analytics**

### ⚖️ Judgment calls — review before answering

**App activity → In-app search history / "Other user-generated content"** —
recipe search text (→ Spoonacular) and AI prompt text (→ Gemini/Groq) are
**transmitted off the device** but the app does **not store** them or tie them
to the account. If you mark these *processed ephemerally / only sent to a
service provider to fulfil the request*, Google's policy may let you omit them.
Conservative path: declare AI prompt text under **Other user-generated
content** with purpose **App functionality** and tick **Processed ephemerally**.
Decide based on whether you consider Spoonacular/Gemini/Groq service providers.

**Audio files → Voice or sound recordings** — recommended answer: **NOT
collected.** Mic audio is handled by the OS `SpeechRecognizer`; the app never
records to disk, never uploads audio to your servers, and the mic is active
only while the user is on Cooking Mode and has tapped the mic. (You still
disclose `RECORD_AUDIO` use in the listing / Privacy Policy §1.3 — but the
Data Safety form is about data *your app* collects.)

### ❌ Do NOT declare (the app does not collect these)
Name (real), phone, address, location (approximate/precise), financial/payment
info, health/fitness, contacts, calendar, photos/videos, files/docs, messages,
web browsing history, installed apps.

---

## Part 3 — Quick console checklist
1. Data safety → **Start** → "Does your app collect or share data?" → **Yes**.
2. Encrypted in transit → **Yes**. Deletion method → **Yes** (in-app + email).
3. Tick data types: **Email address, User IDs, Other user-generated content,
   Device or other IDs, Crash logs, Diagnostics** (+ your decision on search/AI text).
4. For each: set Collected/Shared/Optional + purposes per Part 2.
5. Make sure the **Privacy policy URL** field (separate, in the store listing)
   points to the hosted `docs/privacy-policy.md`
   (https://kartikkh1607.github.io/MyrecipeApp/privacy-policy/).

---

*Maps to `docs/privacy-policy.md`. If you change SDKs or AI providers, update both.*
