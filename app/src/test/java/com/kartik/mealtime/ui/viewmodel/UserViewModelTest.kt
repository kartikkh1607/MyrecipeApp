package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.FavoriteEntity
import com.kartik.mealtime.data.preferences.DietaryPref
import com.kartik.mealtime.data.preferences.RecentRecipe
import com.kartik.mealtime.data.preferences.RecentRecipesRepository
import com.kartik.mealtime.data.preferences.UserPreferences
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

/**
 * Unit tests for [UserViewModel].
 *
 * The VM exposes its StateFlows via `stateIn(..., WhileSubscribed(5_000), ...)`,
 * which stays parked at the initial seed unless a collector keeps the upstream
 * alive. Each test that reads `.value` first launches a no-op collector in
 * [runTest]'s `backgroundScope` so the sharing strategy is satisfied.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModelTest {

    private class FakeFavoriteDao(items: List<FavoriteEntity>) : FavoriteDao {
        private val state = MutableStateFlow(items)
        override fun getAllFlow(): Flow<List<FavoriteEntity>> = state
        override fun getAllSync(): List<FavoriteEntity> = state.value
        override suspend fun insert(entity: FavoriteEntity) {}
        override suspend fun delete(id: String) {}
        override suspend fun deleteAll() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fav(id: String) = FavoriteEntity(
        id = id, name = "Recipe $id", imageUrl = "", category = "",
        rating = 0f, prepTime = 0, cookTime = 0, difficulty = "EASY"
    )

    private fun buildViewModel(
        preferences: Flow<UserPreferences> = flowOf(UserPreferences()),
        recents: Flow<List<RecentRecipe>> = flowOf(emptyList()),
        favorites: List<FavoriteEntity> = emptyList(),
        userPrefsRepo: UserPreferencesRepository = mock(UserPreferencesRepository::class.java),
        recentRepo: RecentRecipesRepository = mock(RecentRecipesRepository::class.java),
    ): Pair<UserViewModel, UserPreferencesRepository> {
        `when`(userPrefsRepo.preferences).thenReturn(preferences)
        `when`(recentRepo.recents).thenReturn(recents)
        val vm = UserViewModel(userPrefsRepo, recentRepo, FakeFavoriteDao(favorites))
        return vm to userPrefsRepo
    }

    /** Keeps every WhileSubscribed-shared StateFlow on the VM alive. */
    private fun CoroutineScope.subscribeAll(vm: UserViewModel) {
        launch { vm.preferences.collect {} }
        launch { vm.recentRecipes.collect {} }
        launch { vm.favoritesCount.collect {} }
    }

    @Test
    fun preferencesExposesTheRepositoryFlow() = runTest(UnconfinedTestDispatcher()) {
        val expected = UserPreferences(displayName = "Kartik", defaultServings = 4)
        val (vm, _) = buildViewModel(preferences = flowOf(expected))
        backgroundScope.subscribeAll(vm)

        assertEquals(expected, vm.preferences.value)
    }

    @Test
    fun recentRecipesExposesTheRepositoryFlow() = runTest(UnconfinedTestDispatcher()) {
        val recents = listOf(
            RecentRecipe(id = "1", name = "Pasta", imageUrl = "", viewedAt = 1L)
        )
        val (vm, _) = buildViewModel(recents = flowOf(recents))
        backgroundScope.subscribeAll(vm)

        assertEquals(recents, vm.recentRecipes.value)
    }

    @Test
    fun favoritesCountReflectsTheDaoFlowSize() = runTest(UnconfinedTestDispatcher()) {
        val (vm, _) = buildViewModel(favorites = listOf(fav("1"), fav("2"), fav("3")))
        backgroundScope.subscribeAll(vm)

        assertEquals(3, vm.favoritesCount.value)
    }

    @Test
    fun toggleDietaryPrefAddsAMissingPref() = runTest(UnconfinedTestDispatcher()) {
        val (vm, repo) = buildViewModel(preferences = flowOf(UserPreferences()))
        backgroundScope.subscribeAll(vm)

        vm.toggleDietaryPref(DietaryPref.VEGAN)

        verify(repo).updateDietaryPrefs(setOf(DietaryPref.VEGAN))
    }

    @Test
    fun toggleDietaryPrefRemovesAnExistingPref() = runTest(UnconfinedTestDispatcher()) {
        val initial = UserPreferences(dietaryPrefs = setOf(DietaryPref.VEGAN, DietaryPref.KETO))
        val (vm, repo) = buildViewModel(preferences = flowOf(initial))
        backgroundScope.subscribeAll(vm)

        vm.toggleDietaryPref(DietaryPref.VEGAN)

        verify(repo).updateDietaryPrefs(setOf(DietaryPref.KETO))
    }

    @Test
    fun updateDisplayNameDelegatesToTheRepository() = runTest(UnconfinedTestDispatcher()) {
        val (vm, repo) = buildViewModel()
        vm.updateDisplayName("Kartik")
        verify(repo).updateDisplayName("Kartik")
    }

    @Test
    fun updateAvatarDelegatesToTheRepository() = runTest(UnconfinedTestDispatcher()) {
        val (vm, repo) = buildViewModel()
        vm.updateAvatar("🍕")
        verify(repo).updateAvatar("🍕")
    }

    @Test
    fun saveProfileDelegatesToTheRepository() = runTest(UnconfinedTestDispatcher()) {
        val (vm, repo) = buildViewModel()
        val edited = UserPreferences(displayName = "Edited", defaultServings = 6)
        vm.saveProfile(edited)
        verify(repo).saveProfile(edited)
    }
}
