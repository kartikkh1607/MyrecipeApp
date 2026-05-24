package com.kartik.mealtime.domain.model

/**
 * Represents the three possible theme settings a user can choose.
 * Stored as an ordinal Int inside DataStore Preferences.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;       // Follow the device system setting

    fun label(): String = when (this) {
        LIGHT  -> "Light"
        DARK   -> "Dark"
        SYSTEM -> "System Default"
    }

    fun emoji(): String = when (this) {
        LIGHT  -> "☀️"
        DARK   -> "🌙"
        SYSTEM -> "📱"
    }
}
