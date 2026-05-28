# 🍳 MealTime

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min_SDK-26-orange?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Target_SDK-36-green?style=for-the-badge"/>
</p>

**MealTime** is a production-ready Android recipe & meal-planning app built entirely with **Jetpack Compose**. It pairs a polished, spring-physics-driven UI with a live recipe database, Firebase-backed accounts, an AI cooking assistant, and a Play Billing premium tier — all backed by a serverless Cloudflare Worker proxy so no API keys ever ship inside the APK.

> Package: `com.kartik.mealtime` &nbsp;•&nbsp; Version: `1.0.1`

---

## ✨ Feature Tour

### 🏠 Recipe Discovery
- **Featured carousel** with auto-scroll, spring-snap pager, and gradient overlays.
- **Category & cuisine browsing** via horizontally scrollable chips.
- **Debounced live search** with paginated results and popular-tag suggestions.
- **Recipe detail** with parallax hero image, full nutrition panel (calories, macros, fibre, sodium, sugar), and tabbed ingredients / instructions.
- **Dietary badges**: Vegetarian, Vegan, Gluten-Free, Keto, Low-Carb, Dairy-Free.

### 👨‍🍳 Cooking Mode
- Distraction-free, full-screen step-by-step guide.
- Keeps the screen awake while cooking (`FLAG_KEEP_SCREEN_ON`).
- Per-step timer with a visual progress ring.

### ❤️ Favorites & 🛒 Smart Shopping List
- Save recipes with one tap; persisted in **Room** and synced across sessions.
- Toggle between staggered grid and detailed list views.
- Add ingredients from any recipe; items are **grouped by recipe** with collapsible sections, per-section progress, swipe-to-delete, and share/copy export.

### 🔐 Accounts & Sync (Firebase)
- **First-launch AuthScreen gate** — sign in before using the app.
- **Google Sign-In via Credential Manager** (the modern replacement for `GoogleSignInClient`) → exchanged for a Firebase ID token.
- Favorites & shopping list sync via **Firestore**; **Crashlytics** + **Analytics** wired in.
- Full **account deletion** flow (Play Data Safety compliant) including partial-data deletion.

### 🤖 AI Cooking Assistant (Premium)
Three AI-powered features gated behind a Play subscription, all routed through the Worker so keys stay server-side:
- **AI Recipe Generation** — describe what you have or feel like; get a complete recipe with nutrition.
- **AI Recipe Remix** — take any existing recipe and rework it for a diet, allergy, or cuisine.
- **AI Meal Planner** — generate a multi-day plan and one-tap export every ingredient to your shopping list.
- **AI Creations** screen archives everything you've generated.
- Falls back to **Groq** if Gemini is unavailable (transparent to the user; disclosed in-app per Play policy).

### 💳 Premium Subscription
- **Google Play Billing v6** — `premium` subscription with `monthly` and `annual` base plans.
- Entitlement is enforced server-side: purchase tokens are validated against the **Play Developer API** and a `premium` Firebase **custom claim** is minted on the user. Real-time Developer Notifications keep it current on renewal / cancellation.
- Local `EntitlementRepository` seam means the rest of the app reads a single source of truth.

### 📺 Ads (Free Tier)
- **AdMob** banner + interstitial slots, gated by `AdConfig.adsEnabled` (disabled entirely for premium users and when production unit IDs are absent).
- **Google UMP** GDPR/EEA consent flow runs on first launch in regulated regions.

### ⚙️ Settings
- Light / Dark / System theme, persisted via **DataStore Preferences**.
- About, attributions, and links to hosted **Privacy Policy**, **Terms**, and **Account Deletion** pages.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0 (JVM 17 toolchain) |
| **UI** | Jetpack Compose · Material 3 · Google Fonts (Playfair Display) · Material Icons Extended |
| **Architecture** | MVVM + Clean Architecture (data / domain / ui) |
| **DI** | **Hilt** (Hilt Navigation Compose for ViewModel scoping) |
| **Async** | Coroutines · StateFlow / Flow |
| **Networking** | Retrofit 3 · OkHttp 5 · Gson · HTTP logging interceptor |
| **AI streaming** | OkHttp directly (SSE) for Gemini / Groq |
| **Image loading** | Coil 2 (Compose) |
| **Local DB** | Room 2.6 (favorites, shopping list, API cache) |
| **Navigation** | Compose Navigation 2.8 — type-safe routes via `kotlinx.serialization` |
| **Auth** | Firebase Auth · AndroidX Credential Manager · `googleid` (Google ID token) |
| **Backend** | Firebase Firestore · Crashlytics · Analytics |
| **Billing** | Google Play Billing KTX |
| **Ads & Consent** | Google Mobile Ads SDK · UMP (User Messaging Platform) |
| **Theme persistence** | DataStore Preferences |
| **Build** | Gradle KTS · KSP (Room + Hilt) · Firebase Crashlytics Gradle plugin |
| **Server** | Cloudflare Worker (free plan, no credit card) |
| **Min / Compile / Target SDK** | 26 / 36 / 36 |

---

## 🏗️ Architecture

Clean Architecture with strict layer boundaries. All cross-layer wiring is done by **Hilt**.

```
app/src/main/java/com/kartik/mealtime/
│
├── data/
│   ├── ads/          # AdConfig + ConsentManager (UMP)
│   ├── analytics/    # Crashlytics / Analytics wrappers
│   ├── billing/      # BillingManager + BillingEntitlementRepository
│   ├── local/        # Room entities, DAOs, AppDatabase, TypeConverters
│   ├── preferences/  # DataStore (theme, first-launch, gates)
│   ├── remote/       # Retrofit (Spoonacular) + AI services (Gemini / Groq) + router
│   │                 # FirebaseAuthInterceptor attaches the Firebase ID token
│   ├── repository/   # Implementations (recipe, favorites, shopping list, entitlement)
│   └── source/       # SampleDataSource — bundled offline fallback
│
├── domain/
│   ├── model/        # Pure Kotlin models
│   ├── repository/   # Repository interfaces
│   └── usecase/      # Use-cases (search, AI generation, etc.)
│
├── di/               # Hilt modules (Database, Network, Repository, Firebase, Analytics)
│
└── ui/
    ├── components/   # Shared composables
    ├── navigation/   # NavGraph + type-safe Route sealed classes
    ├── screens/      # Auth, Home, Search, Recipe, Cooking, Favorites, Shopping,
    │                 # Profile, Settings, Chat, AiCreations, MealPlanner, CategoryDetail
    ├── theme/        # PremiumLight/Dark color schemes, typography, shape, motion tokens
    └── viewmodel/    # MainViewModel + screen-specific ViewModels
```

### Data Flow

```
UI (Compose) ──▶ ViewModel ──▶ UseCase ──▶ Repository
                                                │
                              ┌─────────────────┼──────────────────┐
                              ▼                 ▼                  ▼
                       Cloudflare Worker     Room DB Cache     SampleDataSource
                       (Spoonacular / AI)    (offline cache)   (always available)
```

- **Auth-aware networking**: `FirebaseAuthInterceptor` attaches the Firebase ID token to every Worker call — the Worker rejects un-authenticated traffic.
- **Premium-aware AI**: the Worker checks the `premium` custom claim before forwarding JSON-mode Gemini calls; the client treats `402 premium_required` as the upgrade signal.
- **Offline-first**: API responses are cached in Room (`cached_recipes`); on network failure the repository falls back to cache, then to `SampleDataSource` — the app **never** crashes on a missing key or dead network.

---

## 🗄️ Local Database (Room)

| Table | Purpose |
|---|---|
| `favorites` | Persisted saved recipes |
| `shopping_items` | Shopping-list items grouped by recipe |
| `cached_recipes` | Full Spoonacular detail responses |

Schemas are exported under `app/schemas/` for migration safety.

---

## ☁️ Server: Cloudflare Worker Proxy

API keys are **not** shipped in the APK. The Worker holds the secrets and forwards requests.

```
App ──(Firebase ID token)──► Worker (holds keys) ──► Spoonacular / Gemini / Groq
                                                  └─► Play Developer API (billing/verify)
```

| Route | Forwards to |
|---|---|
| `GET  /spoonacular/<path>` | `api.spoonacular.com/<path>` with `apiKey` |
| `POST /gemini` | Gemini `:generateContent` (JSON mode requires `premium` claim) |
| `POST /groq` | Groq `chat/completions` (fallback) |
| `POST /billing/verify` | Validates a purchase and sets the `premium` custom claim |
| `POST /billing/rtdn` | Pub/Sub push for renewal / cancellation |

Free plan, no credit card, 100k requests/day. Full setup in [`server/cloudflare-worker/README.md`](./server/cloudflare-worker/README.md).

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) **Koala (2024.1.1)** or newer
- **JDK 17+**
- An Android device or emulator running **API 26+**
- A Firebase project (for Auth, Firestore, Crashlytics) — drop the `google-services.json` into `app/`

### 1. Clone

```bash
git clone https://github.com/kartikkh1607/MyrecipeApp.git
cd MyrecipeApp
```

### 2. Configure `local.properties`

The Worker URL is the only required value — the app ships pointed at the production proxy by default.

```properties
# Cloudflare Worker proxy URL (override for dev / staging)
proxy.base.url=https://mealtime-proxy.<your-subdomain>.workers.dev

# AdMob (release only — debug uses Google's baked-in test IDs)
admob.app.id=ca-app-pub-XXXX~XXXX
admob.banner.id=ca-app-pub-XXXX/XXXX
admob.interstitial.id=ca-app-pub-XXXX/XXXX

# Release signing (optional — only needed for signed release builds)
keystore.path=mealtime-release.jks
keystore.store.password=...
keystore.key.alias=...
keystore.key.password=...
```

`local.properties` is gitignored. If `admob.*` values are blank, `AdConfig.adsEnabled` returns `false` for release builds so test IDs can never ship as production.

### 3. Deploy the Worker (optional — only if you want your own backend)

See [`server/cloudflare-worker/README.md`](./server/cloudflare-worker/README.md) — `wrangler login`, three `wrangler secret put` calls, `wrangler deploy`.

### 4. Build & Run

```bash
./gradlew installDebug
```

Or hit **▶ Run** in Android Studio.

---

## 🧪 Testing

- **JVM unit tests**: ViewModels, repositories, sync / favorites — JUnit · Mockito · `kotlinx-coroutines-test` · **Robolectric** (for Android-resource-aware tests).
- **Instrumented tests** (`androidTest` source set): Room migrations + Compose UI flows — AndroidX Test · Espresso · Compose UI Test.
- **Macrobenchmark / Baseline Profiles**: `androidx.benchmark.macro.junit4` + UI Automator.

```bash
./gradlew test                  # unit
./gradlew connectedAndroidTest  # instrumented (device / emulator required)
```

---

## 🎨 Design System

Custom **"Premium Brand"** tokens in `ui/theme/`:

- **Color**: `PremiumLightColorScheme` / `PremiumDarkColorScheme` — curated, no Material defaults.
- **Typography**: Playfair Display (display / headline) + Material 3 body scale.
- **Shape**: Consistent rounded-corner tokens across components.
- **Motion**: Spring-physics tokens (`dampingRatio`, `stiffness`) for iOS-style transitions.
- **Edge-to-edge**: `enableEdgeToEdge()` everywhere; screens use `statusBarsPadding` / `navigationBarsPadding`.

Full reference: [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md).

---

## 📚 Additional Docs

- [`API_SETUP_GUIDE.md`](./API_SETUP_GUIDE.md) — historical Spoonacular setup notes
- [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md) — color, type, motion tokens
- [`SECURITY.md`](./SECURITY.md) — threat model + key-handling rationale
- [`PLAY_DATA_SAFETY.md`](./PLAY_DATA_SAFETY.md) — Data Safety disclosures
- [`server/cloudflare-worker/README.md`](./server/cloudflare-worker/README.md) — Worker setup + billing verification

---

## 🗺️ Roadmap

- [ ] Drag-and-drop weekly meal-planning calendar (visual layer over the AI planner)
- [ ] Advanced filters — max calories, max cook time, dietary intersections
- [ ] User ratings & notes per recipe (local)
- [ ] Grocery store aisle grouping for the shopping list
- [ ] Home-screen widget — today's featured recipe

---

## 🤝 Contributing

PRs welcome. For non-trivial changes please open an issue first to discuss the approach.

1. Fork the repo
2. `git checkout -b feature/your-feature`
3. Commit (the project follows conventional-ish prefixes — `feat:`, `fix:`, `perf:`, `refactor:`, `docs:`, `test:`, `chore:`)
4. `git push origin feature/your-feature`
5. Open a PR

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](./LICENSE) for details.

---

<p align="center">
  Made with ❤️ using Jetpack Compose &nbsp;|&nbsp;
  Powered by <a href="https://spoonacular.com/food-api">Spoonacular</a> · <a href="https://ai.google.dev/">Gemini</a> · <a href="https://groq.com/">Groq</a> &nbsp;|&nbsp;
  Served by <a href="https://workers.cloudflare.com/">Cloudflare Workers</a>
</p>
