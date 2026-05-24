package com.kartik.mealtime.data.preferences

/**
 * Per-user personalization data. Stored in DataStore (local-only for v1 —
 * Firestore sync can be added later via SyncRepository if needed).
 *
 *  - [displayName] — user-chosen name. Shown in greeting + profile.
 *  - [avatarEmoji] — single grapheme cluster shown in the profile avatar circle.
 *  - [dietaryPrefs] — used to bias AI Chef suggestions toward matching recipes.
 *  - [cookingStreakDays] — consecutive days the user has opened a recipe.
 *  - [lastCookedDate] — ISO yyyy-MM-dd string. Used to compute streak transitions.
 *  - [totalRecipesViewed] — lifetime counter shown on profile.
 */
data class UserPreferences(
    val displayName: String = "",
    val avatarEmoji: String = DEFAULT_AVATAR,
    val dietaryPrefs: Set<DietaryPref> = emptySet(),
    val cookingStreakDays: Int = 0,
    val lastCookedDate: String = "",
    val totalRecipesViewed: Int = 0
) {
    companion object {
        const val DEFAULT_AVATAR = "🧑‍🍳"  // 🧑‍🍳

        /** Curated emoji set the user can pick from in EditProfileSheet. */
        val AVAILABLE_AVATARS = listOf(
            "🧑‍🍳",  // 🧑‍🍳
            "👨‍🍳",  // 👨‍🍳
            "👩‍🍳",  // 👩‍🍳
            "🍳",                    // 🍳
            "🍽️",              // 🍽️
            "🍕",                    // 🍕
              "🥗",                    // 🥗
            "🍜",                    // 🍜
            "🥑",                    // 🥑
            "🌮",                    // 🌮
            "🍰",                    // 🍰
            "🍔"                     // 🍔
        )
    }
}

/**
 * Dietary preference flags. User can toggle multiple; the set is fed into
 * AI Chef prompts so suggestions respect the user's diet.
 */
enum class DietaryPref(val label: String, val emoji: String) {
    VEGETARIAN("Vegetarian", "🥕"),    // 🥕
    VEGAN("Vegan", "🌱"),              // 🌱
    GLUTEN_FREE("Gluten-Free", "🌾"),  // 🌾
    DAIRY_FREE("Dairy-Free", "🥛"),    // 🥛
    KETO("Keto", "🥑"),                // 🥑
    LOW_CARB("Low-Carb", "🥦")         // 🥦
}
