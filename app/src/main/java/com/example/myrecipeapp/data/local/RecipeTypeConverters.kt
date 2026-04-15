package com.example.myrecipeapp.data.local

import androidx.room.TypeConverter
import com.example.myrecipeapp.domain.model.Recipe
import com.google.gson.Gson

/**
 * Room TypeConverters that serialise a full [Recipe] object to/from a JSON string.
 * Used exclusively by [CachedRecipeEntity] to store the complete recipe in one column.
 *
 * [gson] is a companion-object singleton — avoids creating a new Gson per converter
 * instance (Room may instantiate converters more than once across threads).
 */
class RecipeTypeConverters {

    @TypeConverter
    fun recipeToJson(recipe: Recipe): String = gson.toJson(recipe)

    @TypeConverter
    fun jsonToRecipe(json: String): Recipe = gson.fromJson(json, Recipe::class.java)

    companion object {
        private val gson = Gson()
    }
}
