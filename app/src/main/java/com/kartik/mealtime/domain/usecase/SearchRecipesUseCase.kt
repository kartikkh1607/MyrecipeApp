package com.kartik.mealtime.domain.usecase

import com.kartik.mealtime.domain.model.SearchResult
import com.kartik.mealtime.domain.repository.RecipeRepository
import javax.inject.Inject

/**
 * Use case: search recipes by keyword.
 * Makes exactly one API call per search — simple, fast, and quota-friendly.
 *
 * API cost: 1 point per search call.
 */
class SearchRecipesUseCase @Inject constructor(
    private val repository: RecipeRepository,
) {
    suspend operator fun invoke(query: String, offset: Int = 0): Result<SearchResult> = runCatching {
        if (query.isBlank()) return@runCatching SearchResult(emptyList(), 0)
        repository.searchRecipes(query, offset = offset, limit = MAX_RESULTS)
    }

    private companion object {
        const val MAX_RESULTS = 20
    }
}
