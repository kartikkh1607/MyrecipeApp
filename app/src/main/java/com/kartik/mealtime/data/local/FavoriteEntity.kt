package com.kartik.mealtime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeDifficulty

/**
 * Room entity — stores only the fields needed to display a recipe card.
 * Full details (ingredients, instructions) are always fetched fresh from the API.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val rating: Float,
    val prepTime: Int,
    val cookTime: Int,
    val difficulty: String   // stored as name() string, converted back via valueOf()
)

// ── Mappers ───────────────────────────────────────────────────────────────────

fun Recipe.toFavoriteEntity() = FavoriteEntity(
    id         = id,
    name       = name,
    imageUrl   = imageUrl,
    category   = category,
    rating     = rating,
    prepTime   = prepTime,
    cookTime   = cookTime,
    difficulty = difficulty.name
)

fun FavoriteEntity.toRecipe() = Recipe(
    id          = id,
    name        = name,
    imageUrl    = imageUrl,
    category    = category,
    rating      = rating,
    prepTime    = prepTime,
    cookTime    = cookTime,
    difficulty  = runCatching { RecipeDifficulty.valueOf(difficulty) }
        .getOrDefault(RecipeDifficulty.MEDIUM),
    // Fields not stored locally — will be loaded from API on RecipeDetailScreen
    description = "",
    cuisine     = "",
    servings    = 0,
    reviewCount = 0,
    ingredients = emptyList(),
    instructions= emptyList(),
    tags        = emptyList()
)
