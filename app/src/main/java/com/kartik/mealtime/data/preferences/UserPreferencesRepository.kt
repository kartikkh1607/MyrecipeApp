package com.kartik.mealtime.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "user_prefs")

/**
 * Persists [UserPreferences] in DataStore. Single source of truth for user
 * personalization data — avatar, display name, dietary prefs, streak, etc.
 *
 * v1 is local-only. Future: sync to Firestore via [SyncRepository] under
 * `users/{uid}/profile/main` so the user's data follows them across devices.
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    val preferences: Flow<UserPreferences> = context.userPrefsDataStore.data.map { prefs ->
        UserPreferences(
            displayName = prefs[KEY_DISPLAY_NAME] ?: "",
            avatarEmoji = prefs[KEY_AVATAR] ?: UserPreferences.DEFAULT_AVATAR,
            dietaryPrefs = parseDietaryPrefs(prefs[KEY_DIETARY_PREFS]),
            allergies = parseAllergies(prefs[KEY_ALLERGIES]),
            skillLevel = parseEnum(prefs[KEY_SKILL_LEVEL], SkillLevel.BEGINNER),
            defaultServings = (prefs[KEY_DEFAULT_SERVINGS] ?: UserPreferences.DEFAULT_SERVINGS)
                .coerceIn(UserPreferences.MIN_SERVINGS, UserPreferences.MAX_SERVINGS),
            spiceLevel = parseEnum(prefs[KEY_SPICE_LEVEL], SpiceLevel.MEDIUM),
            unitSystem = parseEnum(prefs[KEY_UNIT_SYSTEM], UnitSystem.METRIC),
            cookingStreakDays = prefs[KEY_STREAK_DAYS] ?: 0,
            lastCookedDate = prefs[KEY_LAST_COOKED_DATE] ?: "",
            totalRecipesViewed = prefs[KEY_TOTAL_VIEWED] ?: 0
        )
    }

    suspend fun updateDisplayName(name: String) {
        context.userPrefsDataStore.edit { it[KEY_DISPLAY_NAME] = name.trim() }
    }

    suspend fun updateAvatar(emoji: String) {
        context.userPrefsDataStore.edit { it[KEY_AVATAR] = emoji }
    }

    suspend fun updateDietaryPrefs(prefs: Set<DietaryPref>) {
        context.userPrefsDataStore.edit {
            it[KEY_DIETARY_PREFS] = prefs.joinToString(",") { p -> p.name }
        }
    }

    /**
     * Single-transaction profile save. Writes every user-editable field in one
     * DataStore [edit] so observers receive exactly one emission instead of many —
     * prevents the Profile screen from flickering through partially-updated state.
     *
     * Stat fields (streak, last-cooked, total-viewed) are deliberately untouched:
     * they are owned by [recordRecipeView], not the edit sheet.
     */
    suspend fun saveProfile(prefs: UserPreferences) {
        context.userPrefsDataStore.edit {
            it[KEY_DISPLAY_NAME]     = prefs.displayName.trim()
            it[KEY_AVATAR]           = prefs.avatarEmoji
            it[KEY_DIETARY_PREFS]    = prefs.dietaryPrefs.joinToString(",") { p -> p.name }
            it[KEY_ALLERGIES]        = prefs.allergies.joinToString(",") { a -> a.name }
            it[KEY_SKILL_LEVEL]      = prefs.skillLevel.name
            it[KEY_DEFAULT_SERVINGS] = prefs.defaultServings
                .coerceIn(UserPreferences.MIN_SERVINGS, UserPreferences.MAX_SERVINGS)
            it[KEY_SPICE_LEVEL]      = prefs.spiceLevel.name
            it[KEY_UNIT_SYSTEM]      = prefs.unitSystem.name
        }
    }

    /**
     * Whether the user has already passed the first-launch auth gate (either by
     * signing in or by tapping "Maybe later"). Reset to false on sign-out so the
     * gate appears again on the next cold launch.
     */
    val authGateSeen: Flow<Boolean> = context.userPrefsDataStore.data.map { prefs ->
        prefs[KEY_AUTH_GATE_SEEN] ?: false
    }

    suspend fun setAuthGateSeen(seen: Boolean) {
        context.userPrefsDataStore.edit { it[KEY_AUTH_GATE_SEEN] = seen }
    }

    /**
     * Records a recipe view. Updates the streak based on the gap between
     * [lastCookedDate] and today:
     *   - today      → no change (already cooked today)
     *   - yesterday  → streak + 1
     *   - older / "" → streak reset to 1
     * Also increments [totalRecipesViewed].
     */
    suspend fun recordRecipeView() {
        val today = LocalDate.now()
        val todayStr = today.toString()  // ISO yyyy-MM-dd

        context.userPrefsDataStore.edit { prefs ->
            val previousDateStr = prefs[KEY_LAST_COOKED_DATE].orEmpty()
            val currentStreak = prefs[KEY_STREAK_DAYS] ?: 0

            val newStreak = when {
                previousDateStr == todayStr -> currentStreak                  // already counted today
                previousDateStr.isEmpty()   -> 1                              // first ever view
                isYesterday(previousDateStr, today) -> currentStreak + 1      // consecutive day
                else -> 1                                                      // broke the streak
            }

            prefs[KEY_LAST_COOKED_DATE] = todayStr
            prefs[KEY_STREAK_DAYS]      = newStreak
            prefs[KEY_TOTAL_VIEWED]     = (prefs[KEY_TOTAL_VIEWED] ?: 0) + 1
        }
    }

    private fun parseDietaryPrefs(raw: String?): Set<DietaryPref> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { runCatching { DietaryPref.valueOf(it.trim()) }.getOrNull() }
            .toSet()
    }

    private fun parseAllergies(raw: String?): Set<Allergen> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { runCatching { Allergen.valueOf(it.trim()) }.getOrNull() }
            .toSet()
    }

    /** Reads an enum constant by name, falling back to [default] on any mismatch. */
    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, default: T): T {
        if (raw.isNullOrBlank()) return default
        return runCatching { enumValueOf<T>(raw.trim()) }.getOrDefault(default)
    }

    /** True iff [previousDateStr] (ISO) is exactly one day before [today]. */
    private fun isYesterday(previousDateStr: String, today: LocalDate): Boolean {
        return try {
            LocalDate.parse(previousDateStr).plusDays(1) == today
        } catch (e: DateTimeParseException) {
            false
        }
    }

    private companion object {
        val KEY_DISPLAY_NAME     = stringPreferencesKey("display_name")
        val KEY_AVATAR           = stringPreferencesKey("avatar_emoji")
        val KEY_DIETARY_PREFS    = stringPreferencesKey("dietary_prefs")
        val KEY_ALLERGIES        = stringPreferencesKey("allergies")
        val KEY_SKILL_LEVEL      = stringPreferencesKey("skill_level")
        val KEY_DEFAULT_SERVINGS = intPreferencesKey("default_servings")
        val KEY_SPICE_LEVEL      = stringPreferencesKey("spice_level")
        val KEY_UNIT_SYSTEM      = stringPreferencesKey("unit_system")
        val KEY_STREAK_DAYS      = intPreferencesKey("streak_days")
        val KEY_LAST_COOKED_DATE = stringPreferencesKey("last_cooked_date")
        val KEY_TOTAL_VIEWED     = intPreferencesKey("total_viewed")
        val KEY_AUTH_GATE_SEEN   = booleanPreferencesKey("auth_gate_seen")
    }
}
