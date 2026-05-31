package com.kartik.mealtime.di

import com.kartik.mealtime.data.billing.BillingEntitlementRepository
import com.kartik.mealtime.data.local.ThemePreferences
import com.kartik.mealtime.data.remote.AiService
import com.kartik.mealtime.data.remote.AiServiceRouter
import com.kartik.mealtime.data.repository.RecipeRepositoryImpl
import com.kartik.mealtime.domain.repository.EntitlementRepository
import com.kartik.mealtime.domain.repository.RecipeRepository
import com.kartik.mealtime.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
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

    /**
     * Binds the DataStore-backed [ThemePreferences] as the [ThemeRepository] seam so
     * unit tests can inject a pure-Kotlin fake instead of pulling in Robolectric.
     */
    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemePreferences): ThemeRepository

    // Use cases use @Inject constructor and don't need @Provides here — Hilt
    // generates their factories from RecipeRepository (also bound above).
}
