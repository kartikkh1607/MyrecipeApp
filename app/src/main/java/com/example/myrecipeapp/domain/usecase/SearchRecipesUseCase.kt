package com.example.myrecipeapp.domain.usecase

import com.example.myrecipeapp.domain.model.Recipe
import com.example.myrecipeapp.domain.model.SearchResult
import com.example.myrecipeapp.domain.repository.RecipeRepository

/**
 * Use case: search recipes by keyword.
 *
 * Since [pageSize] == [maxResults] (both 50), this always makes exactly **one** API call
 * at default config. The loop is preserved for future flexibility (e.g. load-more).
 *
 * API cost: 1 point per search call.
 */
class SearchRecipesUseCase(
    private val repository: RecipeRepository,
    private val pageSize: Int = 50,
    private val maxResults: Int = 50
) {
    suspend operator fun invoke(query: String): Result<SearchResult> = runCatching {
        if (query.isBlank()) return@runCatching SearchResult(emptyList(), 0)

        val aggregated = mutableListOf<Recipe>()
        var offset = 0
        var total = Int.MAX_VALUE   // sentinel: updated on first response

        while (aggregated.size < maxResults && aggregated.size < total) {
            val batch = pageSize.coerceAtMost(maxResults - aggregated.size)
            val page = repository.searchRecipes(query, offset, batch)
            if (total == Int.MAX_VALUE) total = page.totalResults
            if (page.recipes.isEmpty()) break
            aggregated += page.recipes
            offset += page.recipes.size
        }

        SearchResult(
            recipes = aggregated.take(minOf(total, maxResults)),
            totalResults = if (total == Int.MAX_VALUE) 0 else total
        )
    }
}
