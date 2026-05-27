package com.kartik.mealtime.di

import com.kartik.mealtime.data.billing.BillingEntitlementRepository
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.AiServiceRouter
import com.kartik.mealtime.data.repository.RecipeRepositoryImpl
import com.kartik.mealtime.domain.repository.EntitlementRepository
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.usecase.GetCategoriesUseCase
import com.kartik.mealtime.domain.usecase.GetFeaturedRecipesUseCase
import com.kartik.mealtime.domain.usecase.FindRecipeVideoUseCase
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

    /**
     * Lets the presentation layer depend on the [AiService] abstraction rather than
     * the concrete [AiServiceRouter] — keeps [com.kartik.mealtime.ui.viewmodel.AiViewModel]
     * unit-testable with a fake AI service.
     */
    @Binds
    @Singleton
    abstract fun bindAiService(impl: AiServiceRouter): AiService

    /**
     * Premium entitlement source. Backed by Play Billing: [BillingEntitlementRepository]
     * exposes the DataStore mirror that [com.kartik.mealtime.data.billing.BillingManager]
     * keeps in sync with the server-set `premium` custom claim. (LocalEntitlementRepository
     * is retained for unit tests / a debug dev-unlock.)
     */
    @Binds
    @Singleton
    abstract fun bindEntitlementRepository(impl: BillingEntitlementRepository): EntitlementRepository

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

        @Provides
        fun provideFindRecipeVideoUseCase(repo: RecipeRepository): FindRecipeVideoUseCase =
            FindRecipeVideoUseCase(repo)
    }
}
