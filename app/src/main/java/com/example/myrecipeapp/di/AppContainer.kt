package com.example.myrecipeapp.di

import com.example.myrecipeapp.MyRecipeApplication
import com.example.myrecipeapp.data.local.AppDatabase
import com.example.myrecipeapp.data.remote.NetworkModule
import com.example.myrecipeapp.data.repository.RecipeRepositoryImpl
import com.example.myrecipeapp.domain.repository.RecipeRepository
import com.example.myrecipeapp.domain.usecase.GetCategoriesUseCase
import com.example.myrecipeapp.domain.usecase.GetFeaturedRecipesUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipeDetailsUseCase
import com.example.myrecipeapp.domain.usecase.GetRecipesByCategoryUseCase
import com.example.myrecipeapp.domain.usecase.SearchRecipesUseCase

/**
 * Manual dependency injection container.
 *
 * Each dependency is created lazily and shared as a singleton for the app's lifetime.
 * The ViewModel factory reads use cases and DAOs from here.
 */
object AppContainer {

    // ── Local database ────────────────────────────────────────────────────────
    private val database by lazy {
        AppDatabase.getInstance(MyRecipeApplication.instance)
    }

    val favoriteDao by lazy { database.favoriteDao() }
    val shoppingDao by lazy { database.shoppingDao() }

    // ── Data layer ────────────────────────────────────────────────────────────
    private val apiService by lazy { NetworkModule.provideApiService() }

    val repository: RecipeRepository by lazy {
        RecipeRepositoryImpl(apiService)
    }

    // ── Use cases ─────────────────────────────────────────────────────────────
    val getFeaturedRecipesUseCase by lazy { GetFeaturedRecipesUseCase(repository) }
    val searchRecipesUseCase by lazy { SearchRecipesUseCase(repository) }
    val getRecipeDetailsUseCase by lazy { GetRecipeDetailsUseCase(repository) }
    val getCategoriesUseCase by lazy { GetCategoriesUseCase(repository) }
    val getRecipesByCategoryUseCase by lazy { GetRecipesByCategoryUseCase(repository) }
}
