---
layout: default
title: Privacy Policy
permalink: /privacy-policy/
---

# Privacy Policy for MealTime

**Last updated: May 31, 2026**

Kartik Khandelwal ("we", "us", or "our") operates the MealTime mobile
application (the "App"). This page explains what information the App
collects, how it is used, and the choices you have.

By using the App, you agree to the collection and use of information in
accordance with this policy.

---

## 1. Information we collect

### 1.1 Account information (required to use the App)
Sign-in is **mandatory** — the App does not offer guest access. When you
create an account or sign in we collect:
- **Email address** — used as your login identifier and, for password
  accounts, for the verification link.
- **Password** — only when you choose email/password sign-in. Stored
  exclusively by Firebase Authentication; we never see or store your
  plaintext password. Google Sign-In users authenticate directly with
  Google and we never receive a password.
- **Email-verification status** — for email/password accounts, we hold
  unverified users on a verification screen until their email is verified;
  Google sign-in is treated as already verified.

### 1.2 User content
The following data is synced to your account via **Cloud Firestore** so
you can access it on other devices:
- Recipes you mark as favorites.
- Recipes you generate with the AI assistant.
- Shopping-list items you add.
- Your in-app preferences (e.g. theme, dietary preferences).

This content is associated with your account and is not shared with
other users.

### 1.3 Recipe / AI query data
All recipe and AI traffic is routed through our **Cloudflare Worker
proxy**, which forwards requests to the upstream providers. The Worker
sees the bearer Firebase ID token (used only to authenticate the request
and enforce per-account rate limits and the premium gate); it does not
store request content.

- **Spoonacular API**: when you search for a recipe, your search query
  and the recipe IDs you view are forwarded to Spoonacular to return
  results. No account information is sent to Spoonacular.
- **AI providers (Google Gemini, with Groq as fallback)**: when you use
  the AI features (recipe generation, chat assistant, meal planner), the
  text prompts you send are transmitted to **Google (Gemini API)** for
  inference. If the Gemini request fails and a fallback is configured,
  the same prompt may instead be sent to **Groq** for inference. No
  account information is attached to these requests by the App. Do not
  enter personal information into AI prompts.

### 1.4 Advertising
The free version of the App displays ads served by **Google AdMob**.
- To request and measure ads, the Google Mobile Ads SDK collects your
  device's **Advertising ID** (a resettable identifier), along with
  basic device and ad-interaction information. This is declared in the
  App via the `com.google.android.gms.permission.AD_ID` permission.
- This information may be used by Google to deliver and personalize ads
  and to measure ad performance, in accordance with Google's policies.
- You can reset your Advertising ID or opt out of ad personalization at
  any time in your device settings:
  **Settings → Google → Ads** (on most Android devices).
- For details on how Google uses this data, see
  https://support.google.com/admob/answer/6128543 and
  https://policies.google.com/technologies/partner-sites.

### 1.5 Diagnostics and analytics
The App uses the following Google services:
- **Firebase Analytics** — anonymous usage events (screen views,
  feature usage) used to improve the App. No personally identifying
  information is attached.
- **Firebase Crashlytics** (release builds only) — automatically
  uploads crash reports including device model, OS version, and stack
  traces when the App crashes, so we can fix bugs.

Both services may collect a Firebase installation identifier and basic
device information per Google's policies.

### 1.6 Network requests
All network traffic uses **HTTPS**. The App does not allow cleartext
HTTP traffic.

---

## 2. Information we do **not** collect
- We do not collect your real name, phone number, or address.
- We do not collect location data.
- We do not collect contacts, photos, files, or other personal media.
- We do not sell your personal information for money. Note that, to show
  personalized ads, the Google Mobile Ads SDK shares your Advertising ID
  and ad-interaction data with Google; under some privacy laws (e.g. the
  California CCPA) this may be considered a "sale" or "sharing" of
  personal information. You can opt out of ad personalization as
  described in Section 1.4.

---

## 3. Third-party services
The App relies on the following third-party services. Each has its own
privacy policy:

| Service | Purpose | Privacy policy |
|---|---|---|
| Firebase (Google) | Auth, Firestore, Analytics, Crashlytics | https://policies.google.com/privacy |
| Google AdMob | In-app advertising | https://support.google.com/admob/answer/6128543 |
| Google Gemini API | AI recipe generation and chat (primary) | https://policies.google.com/privacy |
| Groq | AI recipe generation and chat (fallback) | https://groq.com/privacy-policy/ |
| Spoonacular | Recipe search and detail data | https://spoonacular.com/food-api/terms |

---

## 4. Data retention and deletion
- **Account and synced content** is kept while your account is active.
- **You can delete your account and all synced data at any time, directly
  in the App** — go to **Profile → Delete account**. This immediately
  removes your synced data (favorites and shopping list) from our
  Firestore database and deletes your authentication record.
- Alternatively, you can request deletion by emailing us at the address in
  Section 9; we will process such requests within 30 days.
- **Crash and analytics data** is retained for up to 90 days
  (Crashlytics) and up to 14 months (Analytics) per Google's defaults.

---

## 5. Children's privacy
The App is not directed at children under 13, and we do not knowingly
collect personal information from children under 13. The ads shown in
the App are not directed at children. If you believe a child has
provided us information, please contact us and we will delete it.

---

## 6. Permissions used
- `INTERNET` — required to load recipes, sign in, and sync data.
- `AD_ID` (Advertising ID) — used by the Google Mobile Ads SDK to
  request and measure ads (see Section 1.4).

---

## 7. Your rights
Depending on where you live (e.g. EU/UK GDPR, California CCPA), you may
have the right to:
- Access the personal data we hold about you.
- Correct or delete that data.
- Object to or restrict its processing.
- Opt out of personalized advertising.
- Receive a copy of your data in a portable format.

To exercise any of these rights, contact us at the email below.

---

## 8. Changes to this policy
We may update this policy from time to time. Material changes will be
announced inside the App or via the email associated with your
account. The "Last updated" date at the top of this page will always
reflect the latest revision.

---

## 9. Contact

If you have questions about this policy or your data:

**Email:** kartikkhandelwal1234589@gmail.com

---

*This policy is provided as a template. Review it with a lawyer before
publishing to make sure it meets the legal requirements in every
country where the App is available.*
