package com.kartik.mealtime.di

import com.kartik.mealtime.data.repository.RecipeRepositoryImpl
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.GetRecipeDetailsUseCase
import com.kartik.mealtime.domain.usecase.GetRecipesByCategoryUseCase
import com.kartik.mealtime.domain.usecase.SearchRecipesUseCase
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRecipeRepository(impl: RecipeRepositoryImpl): RecipeRepository

    companion object {
        @Provides
        fun provideGetFeaturedRecipesUseCase(repo: RecipeRepository): GetFeaturedRecipesUseCase =
            GetFeaturedRecipesUseCase(repo)

        @Provides
        fun provideSearchRecipesUseCase(repo: RecipeRepository): SearchRecipesUseCase =
            SearchRecipesUseCase(repo)

        @Provides
        fun provideGetRecipeDetailsUseCase(repo: RecipeRepository): GetRecipeDetailsUseCase =
            GetRecipeDetailsUseCase(repo)

        @Provides
        fun provideGetCategoriesUseCase(repo: RecipeRepository): GetCategoriesUseCase =
            GetCategoriesUseCase(repo)

        @Provides
        fun provideGetRecipesByCategoryUseCase(repo: RecipeRepository): GetRecipesByCategoryUseCase =
            GetRecipesByCategoryUseCase(repo)
    }
}
