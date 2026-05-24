package com.kartik.mealtime.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kartik.mealtime.domain.model.Recipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One row in the "Recently Viewed" carousel on HomeScreen.
 * Keep small — image URL is needed for the card thumbnail; everything else
 * is fetched fresh from the cache or API when the user opens the recipe.
 */
data class RecentRecipe(
    val id: String,
    val name: String,
    val imageUrl: String,
    val viewedAt: Long
)

private val Context.recentRecipesDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "recent_recipes")

/**
 * DataStore-backed list of recently-viewed recipes. List is capped at
 * [MAX_RECENTS], deduped by ID, and ordered most-recent-first.
 *
 * Stored as a single JSON string for simplicity — DataStore Preferences
 * doesn't support arbitrary lists natively. The list is small (≤8 items)
 * so JSON overhead is negligible.
 */
@Singleton
class RecentRecipesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private val listType = object : TypeToken<List<RecentRecipe>>() {}.type

    val recents: Flow<List<RecentRecipe>> = context.recentRecipesDataStore.data.map { prefs ->
        val json = prefs[KEY_RECENTS] ?: return@map emptyList()
        runCatching { gson.fromJson<List<RecentRecipe>>(json, listType) }.getOrNull().orEmpty()
    }

    /** Pushes [recipe] to the front; dedupes by id; caps at [MAX_RECENTS]. */
    suspend fun add(recipe: Recipe) {
        context.recentRecipesDataStore.edit { prefs ->
            val current = prefs[KEY_RECENTS]
                ?.let { runCatching { gson.fromJson<List<RecentRecipe>>(it, listType) }.getOrNull() }
                .orEmpty()

            val newEntry = RecentRecipe(
                id = recipe.id,
                name = recipe.name,
                imageUrl = recipe.imageUrl,
                viewedAt = System.currentTimeMillis()
            )

            // Most-recent first, deduped by id, capped at MAX_RECENTS.
            val updated = (listOf(newEntry) + current.filter { it.id != recipe.id })
                .take(MAX_RECENTS)

            prefs[KEY_RECENTS] = gson.toJson(updated)
        }
    }

    suspend fun clear() {
        context.recentRecipesDataStore.edit { it.remove(KEY_RECENTS) }
    }

    private companion object {
        const val MAX_RECENTS = 8
        val KEY_RECENTS = stringPreferencesKey("recent_recipes_json")
    }
}
