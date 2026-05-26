package com.kartik.mealtime.data.preferences

/**
 * Per-user personalization data. Stored in DataStore (local-only for v1 —
 * Firestore sync can be added later via SyncRepository if needed).
 *
 *  - [displayName] — user-chosen name. Shown in greeting + profile.
 *  - [avatarEmoji] — single grapheme cluster shown in the profile avatar circle.
 *  - [dietaryPrefs] — used to bias AI Chef suggestions toward matching recipes.
 *  - [allergies] — ingredients the AI Chef must avoid suggesting.
 *  - [skillLevel] — cooking experience; lets the AI tune recipe complexity.
 *  - [defaultServings] — household size; default portion the AI scales to.
 *  - [spiceLevel] — heat preference passed to the AI.
 *  - [unitSystem] — metric vs imperial measurement display.
 *  - [cookingStreakDays] — consecutive days the user has opened a recipe.
 *  - [lastCookedDate] — ISO yyyy-MM-dd string. Used to compute streak transitions.
 *  - [totalRecipesViewed] — lifetime counter shown on profile.
 */
data class UserPreferences(
    val displayName: String = "",
    val avatarEmoji: String = DEFAULT_AVATAR,
    val dietaryPrefs: Set<DietaryPref> = emptySet(),
    val allergies: Set<Allergen> = emptySet(),
    val skillLevel: SkillLevel = SkillLevel.BEGINNER,
    val defaultServings: Int = DEFAULT_SERVINGS,
    val spiceLevel: SpiceLevel = SpiceLevel.MEDIUM,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val cookingStreakDays: Int = 0,
    val lastCookedDate: String = "",
    val totalRecipesViewed: Int = 0
) {

    /**
     * Single human-readable line describing the user's food profile, injected into
     * AI Chef prompts so suggestions respect diet, allergies, skill, portions, and
     * heat preference. Empty sections are omitted to keep the prompt tight.
     */
    fun aiPersonalizationContext(): String {
        val parts = mutableListOf<String>()
        if (dietaryPrefs.isNotEmpty()) {
            parts += "Diet: " + dietaryPrefs.joinToString(", ") { it.label }
        }
        if (allergies.isNotEmpty()) {
            parts += "Allergic to (never include): " + allergies.joinToString(", ") { it.label }
        }
        parts += "Skill: ${skillLevel.label}"
        parts += "Serves: $defaultServings"
        parts += "Spice: ${spiceLevel.label}"
        parts += "Units: ${unitSystem.label}"
        return parts.joinToString("; ")
    }

    companion object {
        const val DEFAULT_AVATAR = "🧑‍🍳"
        const val DEFAULT_SERVINGS = 2
        const val MIN_SERVINGS = 1
        const val MAX_SERVINGS = 12

        /** Curated emoji set the user can pick from in EditProfileSheet. */
        val AVAILABLE_AVATARS = listOf(
            "🧑‍🍳",
            "👨‍🍳",
            "👩‍🍳",
            "🍳",
            "🍽️",
            "🍕",
            "🥗",
            "🍜",
            "🥑",
            "🌮",
            "🍰",
            "🍔"
        )
    }
}

/**
 * Dietary preference flags. User can toggle multiple; the set is fed into
 * AI Chef prompts so suggestions respect the user's diet.
 */
enum class DietaryPref(val label: String, val emoji: String) {
    VEGETARIAN("Vegetarian", "🥕"),
    VEGAN("Vegan", "🌱"),
    GLUTEN_FREE("Gluten-Free", "🌾"),
    DAIRY_FREE("Dairy-Free", "🥛"),
    KETO("Keto", "🥑"),
    LOW_CARB("Low-Carb", "🥦")
}

/**
 * Common food allergens to avoid. Distinct from [DietaryPref] — these are
 * "never suggest" hard constraints, surfaced to the AI as exclusions.
 */
enum class Allergen(val label: String, val emoji: String) {
    PEANUTS("Peanuts", "🥜"),
    TREE_NUTS("Tree nuts", "🌰"),
    MILK("Milk", "🥛"),
    EGGS("Eggs", "🥚"),
    FISH("Fish", "🐟"),
    SHELLFISH("Shellfish", "🦐"),
    SOY("Soy", "🫛"),
    SESAME("Sesame", "🫓"),
    WHEAT("Wheat", "🌾")
}

/** Cooking experience level — lets the AI tune recipe complexity. */
enum class SkillLevel(val label: String, val emoji: String) {
    BEGINNER("Beginner", "🌱"),
    INTERMEDIATE("Home Cook", "🍳"),
    ADVANCED("Pro Chef", "⭐")
}

/** Heat preference passed to the AI Chef. */
enum class SpiceLevel(val label: String, val emoji: String) {
    MILD("Mild", "🫑"),
    MEDIUM("Medium", "🌶️"),
    HOT("Hot", "🔥")
}

/** Measurement system used when displaying quantities. */
enum class UnitSystem(val label: String) {
    METRIC("Metric"),
    IMPERIAL("Imperial")
}
