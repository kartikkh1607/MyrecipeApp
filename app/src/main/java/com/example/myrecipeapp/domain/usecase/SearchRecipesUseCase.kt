package com.example.myrecipeapp.domain.usecase

import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.SearchResult
import com.example.myrecipeapp.domain.repository.RecipeRepository

/**
 * Use case: search recipes by keyword.
 * Handles pagination internally, aggregating up to [maxResults].
 */
class SearchRecipesUseCase(
    private val repository: RecipeRepository,
    private val pageSize: Int = 50,
    private val maxResults: Int = 50   // 50 = 1 API call/search; 1000 = 10 calls (quota killer)
) {
    suspend operator fun invoke(query: String): Result<SearchResult> = runCatching {
        if (query.isBlank()) return@runCatching SearchResult(emptyList(), 0)

        val aggregated = mutableListOf<Recipe>()
        var offset = 0
        var total = Int.MAX_VALUE

        while (aggregated.size < total && aggregated.size < maxResults) {
            val page = repository.searchRecipes(query, offset, pageSize)
            if (total == Int.MAX_VALUE) total = page.totalResults
            if (page.recipes.isEmpty()) break
            aggregated += page.recipes
            offset += page.recipes.size
        }

        val finalResults = aggregated.take(minOf(total, maxResults))
        SearchResult(finalResults, total)
    }
}
