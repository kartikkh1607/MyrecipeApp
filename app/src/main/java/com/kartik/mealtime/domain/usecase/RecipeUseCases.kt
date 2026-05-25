package com.kartik.mealtime.domain.usecase

import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.repository.RecipeRepository

/**
 * Thin delegating use cases that wrap repository calls in [Result].
 * Each class represents a single, named operation in the domain layer —
 * keeping the ViewModel decoupled from the repository interface directly.
 */

class GetFeaturedRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<FeaturedRecipe>> =
        runCatching { repository.getFeaturedRecipes(forceRefresh) }
}

class GetCategoriesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(): Result<List<RecipeCategory>> = runCatching {
        repository.getCategories()
    }
}

class GetRecipeDetailsUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipeId: String): Result<Recipe?> = runCatching {
        repository.getRecipeDetails(recipeId)
    }
}

class GetRecipesByCategoryUseCase(
    private val repository: RecipeRepository,
    private val defaultLimit: Int = 20  // 20 = 1 fast API call; 50 was wasteful
) {
    suspend operator fun invoke(
        categoryId: String,
        limit: Int = defaultLimit,
        offset: Int = 0,
        append: Boolean = false
    ): Result<List<Recipe>> =
        runCatching {
            repository.getRecipesByCategory(categoryId, limit, offset, append)
        }
}

class FindRecipeVideoUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipeName: String): Result<String?> = runCatching {
        repository.findRecipeVideoId(recipeName)
    }
}
