package com.kartik.mealtime.data.repository

import com.kartik.mealtime.data.local.AiRecipeDao
import com.kartik.mealtime.data.local.AiRecipeSource
import com.kartik.mealtime.data.local.toAiRecipeEntity
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The user's "AI Creations" store. Wraps [AiRecipeDao] and maps between the Room
 * entity and the domain [Recipe].
 *
 * Recipes here already carry an `ai-` id (assigned at generation time), so
 * [RecipeRepositoryImpl] can route detail lookups for those ids straight here
 * instead of hitting the network.
 */
@Singleton
class AiRecipeRepository @Inject constructor(
    private val dao: AiRecipeDao
) {

    /** All saved AI recipes, newest first. */
    val creations: Flow<List<Recipe>> =
        dao.getAllFlow().map { list -> list.map { it.recipe } }

    suspend fun save(recipe: Recipe, source: AiRecipeSource, sourceRecipeId: String? = null) {
        dao.upsert(recipe.toAiRecipeEntity(source, sourceRecipeId))
    }

    suspend fun getById(id: String): Recipe? = dao.getById(id)?.recipe

    suspend fun delete(id: String) = dao.delete(id)
}
