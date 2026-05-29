package com.kartik.mealtime.ui.viewmodel

import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.FavoriteEntity
import com.kartik.mealtime.data.preferences.UserPreferencesRepository
import com.kartik.mealtime.data.repository.FavoritesRepository
import com.kartik.mealtime.data.repository.SyncRepository
import com.kartik.mealtime.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unit tests for [FavoritesViewModel]'s sorting logic (relocated from
 * MainViewModelTest when favorites moved into FavoritesRepository +
 * FavoritesViewModel). Uses a fake DAO behind a real FavoritesRepository so the
 * VM's Room-Flow collection populates the list; UnconfinedTestDispatcher makes
 * the init collect run synchronously.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private class FakeFavoriteDao(private val items: List<FavoriteEntity>) : FavoriteDao {
        override fun getAllFlow(): Flow<List<FavoriteEntity>> = flowOf(items)
        override fun getAllSync(): List<FavoriteEntity> = items
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

    private fun buildViewModel(favorites: List<FavoriteEntity>): FavoritesViewModel {
        val repo = FavoritesRepository(
            favoriteDao = FakeFavoriteDao(favorites),
            cachedRecipeDao = mock(CachedRecipeDao::class.java),
            analytics = mock(AnalyticsHelper::class.java),
            userRepository = mock(UserRepository::class.java),
            syncRepository = mock(SyncRepository::class.java)
        )
        val userPrefs = mock(UserPreferencesRepository::class.java).apply {
            `when`(favoritesGridMode).thenReturn(flowOf(false))
        }
        return FavoritesViewModel(repo, userPrefs)
    }

    private fun fav(
        id: String, name: String, rating: Float, prep: Int, cook: Int, difficulty: String
    ) = FavoriteEntity(
        id = id, name = name, imageUrl = "", category = "",
        rating = rating, prepTime = prep, cookTime = cook, difficulty = difficulty
    )

    private val sortFixture = listOf(
        fav("1", "Banana", rating = 3f, prep = 10, cook = 10, difficulty = "HARD"),
        fav("2", "apple", rating = 5f, prep = 5, cook = 5, difficulty = "EASY"),
        fav("3", "Cherry", rating = 4f, prep = 20, cook = 20, difficulty = "MEDIUM")
    )

    private fun sortedIds(vm: FavoritesViewModel) = vm.sortedFavoriteRecipes.value.map { it.id }

    @Test
    fun `sort NAME_AZ orders case-insensitively`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.NAME_AZ)
        assertEquals(listOf("2", "1", "3"), sortedIds(vm))  // apple, Banana, Cherry
    }

    @Test
    fun `sort NAME_ZA reverses name order`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.NAME_ZA)
        assertEquals(listOf("3", "1", "2"), sortedIds(vm))
    }

    @Test
    fun `sort RATING is highest first`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.RATING)
        assertEquals(listOf("2", "3", "1"), sortedIds(vm))  // 5, 4, 3
    }

    @Test
    fun `sort COOK_TIME is quickest first by prep plus cook`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.COOK_TIME)
        assertEquals(listOf("2", "1", "3"), sortedIds(vm))  // 10, 20, 40
    }

    @Test
    fun `sort DIFFICULTY is easiest first`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.DIFFICULTY)
        assertEquals(listOf("2", "3", "1"), sortedIds(vm))  // EASY, MEDIUM, HARD
    }

    @Test
    fun `sort RECENTLY_ADDED preserves source order`() {
        val vm = buildViewModel(sortFixture)
        vm.setFavoritesSortOrder(FavoritesViewModel.FavoritesSortOrder.RECENTLY_ADDED)
        assertEquals(listOf("1", "2", "3"), sortedIds(vm))
    }
}
