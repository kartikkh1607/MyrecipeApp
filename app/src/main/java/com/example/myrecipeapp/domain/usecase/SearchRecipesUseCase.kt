package com.example.myrecipeapp.domain.usecase

import com.example.myrecipeapp.domain.model.SearchResult
import com.example.myrecipeapp.domain.repository.RecipeRepository

/**
 * Use case: search recipes by keyword.
 * Makes exactly one API call per search — simple, fast, and quota-friendly.
 *
 * API cost: 1 point per search call.
 */
class SearchRecipesUseCase(
    private val repository: RecipeRepository,
    private val maxResults: Int = 20
) {
    suspend operator fun invoke(query: String, offset: Int = 0): Result<SearchResult> = runCatching {
        if (query.isBlank()) return@runCatching SearchResult(emptyList(), 0)
        repository.searchRecipes(query, offset = offset, limit = maxResults)
    }
}
