package com.kartik.mealtime.data.local

import androidx.room.TypeConverter
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.serialization.json.Json

/**
 * Room TypeConverters that serialise a full [Recipe] object to/from a JSON string.
 * Used exclusively by [CachedRecipeEntity] to store the complete recipe in one column.
 *
 * The [Json] instance is lenient so a future field addition on [Recipe] doesn't
 * break reads of older cached entries (entries written before the field existed
 * just fall back to the field's default).
 */
class RecipeTypeConverters {

    @TypeConverter
    fun recipeToJson(recipe: Recipe): String = json.encodeToString(Recipe.serializer(), recipe)

    @TypeConverter
    fun jsonToRecipe(string: String): Recipe = json.decodeFromString(Recipe.serializer(), string)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }
}
