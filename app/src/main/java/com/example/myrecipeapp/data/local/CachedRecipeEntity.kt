package com.example.myrecipeapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.myrecipeapp.domain.model.Recipe

/**
 * Room entity that stores a full [Recipe] (ingredients + instructions included) as a JSON blob.
 *
 * Purpose: when the user opens a recipe detail while online, we save the full data here.
 * If they open the same recipe offline later, we serve it from this cache instead of
 * showing an empty detail screen.
 *
 * The JSON serialisation is handled by [RecipeTypeConverters].
 * [cachedAt] is the epoch-millis timestamp of the last successful cache write.
 */
@Entity(tableName = "cached_recipes")
data class CachedRecipeEntity(
    @PrimaryKey val id: String,
    val recipe: Recipe,          // stored as JSON via RecipeTypeConverters
    val cachedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** Cached recipes older than 24 h are considered stale and should be re-fetched. */
        const val CACHE_MAX_AGE_MS = 24 * 60 * 60 * 1_000L
    }

    fun isExpired(): Boolean = System.currentTimeMillis() - cachedAt > CACHE_MAX_AGE_MS
}

// ── Mappers ───────────────────────────────────────────────────────────────────

fun Recipe.toCachedEntity() = CachedRecipeEntity(
    id = id,
    recipe = this,
    cachedAt = System.currentTimeMillis()
)

fun CachedRecipeEntity.toRecipe(): Recipe = recipe
