package com.kartik.mealtime.domain.usecase

import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.Recipe
import com.kartik.mealtime.domain.model.RecipeCategory
import com.kartik.mealtime.domain.repository.RecipeRepository
import javax.inject.Inject

/**
 * Thin delegating use cases that wrap repository calls in [Result].
 * Each class represents a single, named operation in the domain layer —
 * keeping the ViewModel decoupled from the repository interface directly.
 *
 * @Inject on each constructor lets Hilt generate the factory automatically;
 * see [com.kartik.mealtime.di.RepositoryModule] — the matching @Provides
 * methods were removed.
 */

class GetFeaturedRecipesUseCase @Inject constructor(private val repository: RecipeRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): Result<List<FeaturedRecipe>> =
        runCatching { repository.getFeaturedRecipes(forceRefresh) }
}

class GetCategoriesUseCase @Inject constructor(private val repository: RecipeRepository) {
    suspend operator fun invoke(): Result<List<RecipeCategory>> = runCatching {
        repository.getCategories()
    }
}

class GetRecipeDetailsUseCase @Inject constructor(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipeId: String): Result<Recipe?> = runCatching {
        repository.getRecipeDetails(recipeId)
    }
}

class GetRecipesByCategoryUseCase @Inject constructor(
    private val repository: RecipeRepository,
) {
    suspend operator fun invoke(
        categoryId: String,
        limit: Int = DEFAULT_LIMIT, // 20 = 1 fast API call; 50 was wasteful
        offset: Int = 0,
        append: Boolean = false
    ): Result<List<Recipe>> =
        runCatching {
            repository.getRecipesByCategory(categoryId, limit, offset, append)
        }

    private companion object {
        const val DEFAULT_LIMIT = 20
    }
}

class FindRecipeVideoUseCase @Inject constructor(private val repository: RecipeRepository) {
    suspend operator fun invoke(recipeName: String): Result<String?> = runCatching {
        repository.findRecipeVideoId(recipeName)
    }
}
