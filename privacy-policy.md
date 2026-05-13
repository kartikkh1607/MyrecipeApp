# Privacy Policy for MealTime

**Last updated: May 13, 2026**

Kartik Khandelwal ("we", "us", or "our") operates the MealTime mobile
application (the "App"). This page explains what information the App
collects, how it is used, and the choices you have.

By using the App, you agree to the collection and use of information in
accordance with this policy.

---

## 1. Information we collect

### 1.1 Account information (only if you sign in)
If you choose to create an account, we collect:
- **Email address** — used as your login identifier.
- **Password** — stored only by Firebase Authentication; we never see
  or store your plaintext password.

You can use the App without signing in; in that case no account data is
collected.

### 1.2 User content
If you are signed in, the following data is synced to your account via
**Cloud Firestore** so you can access it on other devices:
- Recipes you mark as favorites.
- Recipes you generate with the AI assistant.
- Shopping-list items you add.
- Your in-app preferences (e.g. theme).

This content is associated with your account and is not shared with
other users.

### 1.3 Microphone / voice input
With your explicit permission, the App uses your device's microphone in
**Cooking Mode** so you can advance recipe steps hands-free with voice
commands (e.g. "next", "back", "repeat").
- Audio is processed by **Android's built-in speech recognizer**
  (`android.speech.SpeechRecognizer`). On most devices this is provided
  by Google and audio may be transmitted to Google for transcription
  per Google's own privacy policy.
- The App itself does **not** record audio to disk and does **not** send
  audio to our servers.
- The microphone is only active while you are on the Cooking Mode screen
  and have explicitly tapped the mic button.

### 1.4 Recipe / AI query data
- **Spoonacular API**: when you search for a recipe, your search query
  and the recipe IDs you view are sent to Spoonacular's API to return
  results. No account information is sent.
- **Google Gemini API**: when you use the AI features (recipe
  generation, chat assistant), the text prompts you send are
  transmitted to Google for inference. No account information is
  attached to these requests by the App. Do not enter personal
  information into AI prompts.

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
- We do not show ads and do not use advertising or tracking SDKs.
- We do not sell your data to third parties.

---

## 3. Third-party services
The App relies on the following third-party services. Each has its own
privacy policy:

| Service | Purpose | Privacy policy |
|---|---|---|
| Firebase (Google) | Auth, Firestore, Analytics, Crashlytics | https://policies.google.com/privacy |
| Google Gemini API | AI recipe generation and chat | https://policies.google.com/privacy |
| Spoonacular | Recipe search and detail data | https://spoonacular.com/food-api/terms |
| Android Speech Recognizer (Google) | Voice command transcription | https://policies.google.com/privacy |

---

## 4. Data retention and deletion
- **Account and synced content** is kept while your account is active.
- **You can delete your account at any time** from the Profile screen
  in the App, which removes your authentication record and your
  synced data from our Firestore database.
- You can also email us at the address below to request deletion.
- **Crash and analytics data** is retained for up to 90 days
  (Crashlytics) and up to 14 months (Analytics) per Google's defaults.

---

## 5. Children's privacy
The App is not directed at children under 13, and we do not knowingly
collect personal information from children under 13. If you believe a
child has provided us information, please contact us and we will
delete it.

---

## 6. Permissions used
- `INTERNET` — required to load recipes, sign in, and sync data.
- `RECORD_AUDIO` — used only when you activate voice commands in
  Cooking Mode; can be denied without affecting the rest of the App.

---

## 7. Your rights
Depending on where you live (e.g. EU/UK GDPR, California CCPA), you may
have the right to:
- Access the personal data we hold about you.
- Correct or delete that data.
- Object to or restrict its processing.
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
