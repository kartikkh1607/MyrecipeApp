# 🍳 MyRecipeApp

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Jetpack_Compose-Latest-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white"/>
  <img src="https://img.shields.io/badge/Min_SDK-26-orange?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Target_SDK-35-green?style=for-the-badge"/>
</p>

A **premium Android recipe application** built entirely with **Jetpack Compose**. It features a stunning, modern UI characterized by buttery-smooth spring physics, parallax scroll effects, parallax hero images, glassmorphic headers, and rich micro-interactions — all while connecting to a live recipe database via the **Spoonacular API** with a robust offline-first fallback strategy.

---

## 📸 App Highlights

| Home & Featured Carousel | Recipe Details | Cooking Mode |
|:---:|:---:|:---:|
| Dynamic hero carousel with spring-physics snap | Full nutrition info, ingredients & steps | Distraction-free step-by-step guide |

| Search | Favorites | Shopping List |
|:---:|:---:|:---:|
| Debounced live search + pagination | Staggered grid / list toggle view | Grouped by recipe, swipe-to-delete |

---

## ✨ Key Features

### 🏠 Home Screen
- **Featured Recipe Carousel** with auto-scroll, spring-snap physics, and custom gradient overlays.
- **Category browsing** with horizontally scrollable chip filters.
- **Recipe cards** displaying calorie count, difficulty badge, prep time, and rating.

### 🔍 Smart Search
- Debounced live search with instant feedback to avoid excessive API calls.
- **Paginated results** — scroll to the bottom to load more recipes.
- Popular tag suggestions in the empty state for quick discovery.

### 📖 Recipe Detail
- Full-bleed parallax hero image with an iOS-style top bar (back + favourite button).
- Complete **Nutrition Panel** (calories, protein, carbs, fat, fibre, sodium, sugar).
- Tabbed layout for **Ingredients** and **Step-by-step Instructions**.
- Dietary badges: Vegetarian 🌿, Vegan 🌱, Gluten-Free, Keto, Low-Carb, Dairy-Free.
- One-tap **Add to Shopping List** per ingredient.

### 👨‍🍳 Cooking Mode
- Distraction-free, full-screen step-by-step guide.
- **Keeps screen awake** (`WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`) while you cook.
- Per-step timer with visual progress ring.
- Swipe / button navigation between steps.

### ❤️ Favorites
- Save any recipe with a single tap; persisted locally via **Room**.
- Toggle between a **staggered grid** and a detailed **list view**.
- Long-press to bulk-remove favourites.

### 🛒 Smart Shopping List
- Add individual ingredients directly from any recipe's detail screen.
- Items are **grouped by recipe** with collapsible sections.
- Check off items as you shop, with a progress bar per section.
- **Swipe-to-delete** individual items; one-tap clear per section or the whole list.
- Share / copy the list to any app.

### ⚙️ Settings
- **Light / Dark / System** theme toggle, persisted via **DataStore Preferences**.
- App info, version, and attribution links.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0 |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Architecture** | MVVM + Clean Architecture (Domain / Data / UI) |
| **Async** | Kotlin Coroutines & StateFlow / Flow |
| **Networking** | Retrofit 3 + OkHttp (with HTTP logging interceptor) |
| **JSON Parsing** | Gson via Retrofit converter |
| **Image Loading** | Coil 2 (Compose) |
| **Local Database** | Room 2.6 (Favorites, Shopping List, API cache) |
| **Navigation** | Compose Navigation 2.8 — **type-safe routes** via `kotlinx.serialization` |
| **Dependency Injection** | Manual DI via `MyRecipeApplication` singleton graph |
| **Theme Persistence** | AndroidX DataStore Preferences |
| **Build System** | Gradle (KTS) + KSP (for Room code generation) |
| **Min / Target SDK** | 26 / 35 |

---

## 🏗️ Architecture Overview

The project follows **Clean Architecture** with three distinct layers:

```
app/src/main/java/com/example/myrecipeapp/
│
├── data/
│   ├── local/          # Room entities, DAOs, AppDatabase, TypeConverters
│   ├── remote/         # Retrofit API service, DTOs, DTO mappers
│   ├── repository/     # RecipeRepositoryImpl (merges API + cache + local)
│   └── source/         # SampleDataSource (bundled offline fallback)
│
├── domain/
│   ├── model/          # Pure Kotlin data models (Recipe, Ingredient, etc.)
│   ├── repository/     # RecipeRepository interface
│   └── usecase/        # RecipeUseCases, SearchRecipesUseCase
│
├── di/                 # Manual dependency graph wired in Application class
│
└── ui/
    ├── navigation/     # NavGraph, type-safe Route sealed classes
    ├── screens/        # All 11 composable screens
    ├── theme/          # Color schemes, typography, shape, animation tokens
    └── viewmodel/      # MainViewModel (shared), screen-specific VMs
```

### Data Flow

```
UI (Compose) ──▶ ViewModel ──▶ UseCase ──▶ Repository
                                                │
                              ┌─────────────────┼──────────────────┐
                              ▼                 ▼                  ▼
                        Spoonacular API     Room DB Cache     SampleDataSource
                        (live network)   (offline cache)     (always available)
```

- **On success**: API response is mapped via `DtoMappers.kt` → domain model, then cached in `CachedRecipeEntity` for offline access.
- **On failure / no key**: Falls back to `SampleDataSource` so the app **never crashes**.

---

## 🗄️ Local Database (Room v2)

Three core tables power offline-first functionality:

| Table | Purpose |
|---|---|
| `favorites` | Persists saved recipes across sessions |
| `shopping_items` | Individual ingredients added to the shopping list |
| `cached_recipes` | Full detail responses cached from the Spoonacular API |

---

## 🚀 Getting Started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) **Koala (2024.1.1)** or newer
- **JDK 17+**
- An Android device or emulator running **API 26+**

### 1. Clone the Repository

```bash
git clone https://github.com/kartikkh1607/MyrecipeApp.git
cd MyrecipeApp
```

### 2. Open in Android Studio

Open the cloned folder as a project in Android Studio. Let Gradle sync complete.

### 3. Configure the Spoonacular API Key

The app works out of the box with **bundled sample data**. To unlock live API recipes:

1. Create a free account at [spoonacular.com/food-api](https://spoonacular.com/food-api) and copy your API key.
2. Open (or create) `local.properties` in the **project root** (not the `app/` directory).
3. Add the following line:

```properties
spoonacular.api.key=YOUR_API_KEY_HERE
```

4. Re-sync Gradle — the key is injected securely into `BuildConfig.SPOONACULAR_API_KEY` at compile time and is **excluded from version control** via `.gitignore`.

> 📄 See `API_SETUP_GUIDE.md` for a detailed walkthrough.

### 4. Build & Run

Click **▶ Run** in Android Studio, or:

```bash
./gradlew installDebug
```

---

## 📦 Key Dependencies

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:3.0.0")
implementation("com.squareup.retrofit2:converter-gson:3.0.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Image Loading
implementation("io.coil-kt:coil-compose:2.7.0")

// Room Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Navigation (type-safe)
implementation("androidx.navigation:navigation-compose:2.8.5")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

// DataStore (theme prefs)
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Material Icons Extended
implementation("androidx.compose.material:material-icons-extended")
```

---

## 🎨 Design System

The app uses a custom **"Premium Brand"** design system defined in `ui/theme/`:

- **Color Schemes**: `PremiumDarkColorScheme` & `PremiumLightColorScheme` — rich, curated palettes with no generic defaults.
- **Typography**: Google Fonts–inspired type scale with distinct display, headline, and body weights.
- **Shape**: Rounded corners with consistent radius tokens across all components.
- **Animation**: Custom spring-physics tokens (`dampingRatio`, `stiffness`) for iOS-style fluid transitions.
- **Edge-to-Edge**: `enableEdgeToEdge()` is active; all screens use `statusBarsPadding` / `navigationBarsPadding` to render behind system bars correctly.

A full design reference is available in [`DESIGN_SYSTEM.md`](./DESIGN_SYSTEM.md).

---

## 🗺️ Roadmap

- [ ] **Meal Planner** — drag-and-drop weekly meal planning calendar.
- [ ] **Advanced Filters** — dietary tags, max calories, max cook time, cuisine filters.
- [ ] **User Ratings & Reviews** — local star-rating with notes per recipe.
- [ ] **Grocery Store Integration** — smart grouping of shopping items by aisle/category.
- [ ] **Widget Support** — home-screen widget displaying today's featured recipe.

---

## 🤝 Contributing

Pull requests are welcome! For major changes, please open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create your feature branch: `git checkout -b feature/AmazingFeature`
3. Commit your changes: `git commit -m 'Add AmazingFeature'`
4. Push to the branch: `git push origin feature/AmazingFeature`
5. Open a Pull Request

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](./LICENSE) for more information.

---

<p align="center">Made with ❤️ using Jetpack Compose &nbsp;|&nbsp; Powered by <a href="https://spoonacular.com/food-api">Spoonacular API</a></p>
