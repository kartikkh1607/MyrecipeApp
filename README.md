# 🍳 MyRecipeApp

A beautiful, premium Android application built entirely with **Jetpack Compose**. It features a stunning, modern iOS-style user interface characterized by buttery-smooth spring physics, parallax scroll effects, and rich micro-interactions.

Whether you're exploring spontaneous featured dishes, searching for a specific craving, or following a recipe step-by-step in the kitchen, **MyRecipeApp** provides a fully optimized and seamless cooking experience.

---

## ✨ Key Features

- **Pristine UI/UX**: Designed around modern design language with custom spring-damping animations, glassmorphic headers, and haptic feedback across every major interaction.
- **Smart Shopping List**: Add ingredients directly from any recipe. The list groups items intelligently by recipe, offers swipe-to-delete with progress tracking, and lets you quickly copy/share your cart.
- **Cooking Mode**: A distraction-free step-by-step cooking interface that prevents your screen from sleeping while you cook.
- **Favorites Management**: Save your beloved recipes with a single tap. View them in an elegant staggered grid or a detailed list view with preserved state.
- **Dynamic Search**: Instant search feedback with debouncing, integrated empty states, and popular tag suggestions.
- **Robust API Integration**: Powered by the **Spoonacular Food API** with a seamless local-fallback strategy when the daily API quota is exhausted.

---

## 🛠️ Tech Stack

- **UI Framework:** Kotlin + Jetpack Compose
- **Design System:** Material Design 3 + Custom Animation Physics
- **Architecture:** MVVM (Model-View-ViewModel) + Single Source of Truth
- **Networking:** Retrofit 2 + OkHttp + Gson
- **Image Loading:** Coil (Compose)
- **Navigation:** Compose Navigation (Type-safe)
- **Asynchronous Data:** Kotlin Coroutines & Flows

---

## 🚀 Getting Started

To run this project on your local machine, follow these steps:

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Koala or newer recommended)
- JDK 17+

### Installation & API Setup

This app utilizes the Spoonacular API for live recipe data. 

1. **Clone the repository:**
   ```bash
   git clone https://github.com/kartikkh1607/MyrecipeApp.git
   ```
2. **Open the project** in Android Studio.
3. **Configure the API Key**:
   Because the API key is secured, you must provide your own key to build real network requests.
   - Please read the `API_SETUP_GUIDE.md` included in the root directory for instructions on how to generate a key and insert it into your `local.properties` file.
   - If you do not configure an API key, the app gracefully falls back to bundled sample data so it never crashes!
4. **Build and Run** the app on an emulator or physical device via Android Studio.

---

## 📸 Screenshots
*(Coming Soon - Add your screenshots here to showcase the beautiful UI!)*

---

## 💡 Future Roadmap
- Local Persistence Migration (Room Database) for offline-first Shopping Lists & Favorites.
- Enhanced Dietary & Allergy Filters.
- Dark Theme optimizations. 

---

Made with ❤️ using Jetpack Compose.
