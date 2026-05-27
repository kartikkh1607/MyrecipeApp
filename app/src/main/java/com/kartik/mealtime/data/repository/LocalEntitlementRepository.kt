package com.kartik.mealtime.data.repository

import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder [EntitlementRepository] backed by the local DataStore flag in
 * [UserPreferencesRepository]. Premium defaults to false (locked).
 *
 * This exists so premium features can be built and gated NOW, before Play Billing
 * is implemented (blocked on Play Console verification — see the deferred freemium
 * plan). When billing lands, bind a billing-backed implementation in
 * [com.kartik.mealtime.di.RepositoryModule] instead; nothing else changes.
 */
@Singleton
class LocalEntitlementRepository @Inject constructor(
    private val userPrefs: UserPreferencesRepository
) : EntitlementRepository {

    override val isPremium: Flow<Boolean> = userPrefs.isPremium

    override suspend fun setPremiumOverride(enabled: Boolean) = userPrefs.setPremium(enabled)
}
