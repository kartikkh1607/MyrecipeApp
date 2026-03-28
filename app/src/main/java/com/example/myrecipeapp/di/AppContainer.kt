package com.example.myrecipeapp.di

import com.example.myrecipeapp.data.remote.NetworkModule
import com.example.myrecipeapp.data.repository.RecipeRepositoryImpl
import com.example.myrecipeapp.domain.repository.RecipeRepository
import com.example.myrecipeapp.domain.usecase.*

/**
 * Manual dependency injection container.
 *
 * Each dependency is created lazily and shared as a singleton for the app's lifetime.
 * The ViewModel factory reads use cases from here.
 *
 * No Hilt / Dagger is needed at this scale — this is intentionally simple.
 */
object AppContainer {

    // ── Data layer ────────────────────────────────────────────────────────────
    private val apiService by lazy { NetworkModule.provideApiService() }

    val repository: RecipeRepository by lazy {
        RecipeRepositoryImpl(apiService)
    }

    // ── Use cases ─────────────────────────────────────────────────────────────
    val getFeaturedRecipesUseCase by lazy { GetFeaturedRecipesUseCase(repository) }
    val searchRecipesUseCase      by lazy { SearchRecipesUseCase(repository) }
    val getRecipeDetailsUseCase   by lazy { GetRecipeDetailsUseCase(repository) }
    val getCategoriesUseCase      by lazy { GetCategoriesUseCase(repository) }
    val getRecipesByCategoryUseCase by lazy { GetRecipesByCategoryUseCase(repository) }
}
