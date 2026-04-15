package com.example.myrecipeapp.domain.usecase

import com.example.myrecipeapp.domain.model.FeaturedRecipe
import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.RecipeCategory
import com.example.myrecipeapp.domain.repository.RecipeRepository

/**
 * Thin delegating use cases that wrap repository calls in [Result].
 * Each class represents a single, named operation in the domain layer —
 * keeping the ViewModel decoupled from the repository interface directly.
 */

class GetFeaturedRecipesUseCase(private val repository: RecipeRepository) {
    suspend operator fun invoke(): Result<List<FeaturedRecipe>> = runCatching {
        repository.getFeaturedRecipes()
    }
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
        limit: Int = defaultLimit
    ): Result<List<Recipe>> =
        runCatching {
            repository.getRecipesByCategory(categoryId, limit)
        }
}
