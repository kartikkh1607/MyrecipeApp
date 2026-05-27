package com.kartik.mealtime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartik.mealtime.domain.model.Recipe

/**
 * A recipe the user produced with AI (generation, remix, or photo) and chose to
 * keep. Stores the full [Recipe] as JSON (via [RecipeTypeConverters]) so it opens
 * in the normal detail screen offline.
 *
 * Distinct from [CachedRecipeEntity]: that is a transient 24h offline cache of
 * API recipes; this is the user's permanent "AI Creations" collection.
 *
 * [source] records how it was made (for the badge/filtering); [sourceRecipeId] is
 * the id of the recipe a REMIX was derived from, when applicable.
 */
@Entity(tableName = "ai_recipes")
data class AiRecipeEntity(
    @PrimaryKey val id: String,
    val recipe: Recipe,            // stored as JSON via RecipeTypeConverters
    val source: String,            // AiRecipeSource.name
    val sourceRecipeId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/** How an AI recipe came to be — drives the source badge and any filtering. */
enum class AiRecipeSource { GENERATED, REMIX, PHOTO }

// ── Mappers ───────────────────────────────────────────────────────────────────

fun Recipe.toAiRecipeEntity(
    source: AiRecipeSource,
    sourceRecipeId: String? = null,
) = AiRecipeEntity(
    id = id,
    recipe = this,
    source = source.name,
    sourceRecipeId = sourceRecipeId,
)

fun AiRecipeEntity.toRecipe(): Recipe = recipe
