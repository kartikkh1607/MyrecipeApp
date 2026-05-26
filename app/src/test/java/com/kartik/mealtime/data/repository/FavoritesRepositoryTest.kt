package com.kartik.mealtime.data.repository

import com.google.firebase.auth.FirebaseUser
import com.kartik.mealtime.data.analytics.AnalyticsHelper
import com.kartik.mealtime.data.local.CachedRecipeDao
import com.kartik.mealtime.data.local.CachedRecipeEntity
import com.kartik.mealtime.data.local.FavoriteDao
import com.kartik.mealtime.data.local.FavoriteEntity
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions

/**
 * Unit tests for [FavoritesRepository] — the single source of truth for favorites.
 *
 * The two DAOs are in-memory fakes (so the `favorites` Flow and the local list
 * are real), while the analytics / auth / Firestore-sync collaborators are
 * Mockito mocks we verify against. All methods are plain suspend funcs, so every
 * test runs inside `runTest`; signed-in vs. signed-out is driven via
 * [UserRepository.currentUser].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesRepositoryTest {

    private class FakeFavoriteDao : FavoriteDao {
        val items = MutableStateFlow<List<FavoriteEntity>>(emptyList())
        override fun getAllFlow(): Flow<List<FavoriteEntity>> = items
        override fun getAllSync(): List<FavoriteEntity> = items.value
        override suspend fun insert(entity: FavoriteEntity) {
            // Mirrors Room's INSERT OR REPLACE.
            items.value = items.value.filterNot { it.id == entity.id } + entity
        }
        override suspend fun delete(id: String) { items.value = items.value.filterNot { it.id == id } }
        override suspend fun deleteAll() { items.value = emptyList() }
    }

    private class FakeCachedRecipeDao : CachedRecipeDao {
        val upserts = mutableListOf<CachedRecipeEntity>()
        override suspend fun upsert(entity: CachedRecipeEntity) { upserts += entity }
        override suspend fun getById(id: String): CachedRecipeEntity? = null
        override suspend fun count(): Int = 0
        override suspend fun evictOldest(count: Int) {}
        override suspend fun deleteAll() {}
    }

    private lateinit var favoriteDao: FakeFavoriteDao
    private lateinit var cachedDao: FakeCachedRecipeDao
    private lateinit var analytics: AnalyticsHelper
    private lateinit var userRepository: UserRepository
    private lateinit var syncRepository: SyncRepository

    @Before
    fun setUp() {
        favoriteDao = FakeFavoriteDao()
        cachedDao = FakeCachedRecipeDao()
        analytics = mock(AnalyticsHelper::class.java)
        userRepository = mock(UserRepository::class.java)
        syncRepository = mock(SyncRepository::class.java)
    }

    private fun signInAs(uid: String) {
        val user = mock(FirebaseUser::class.java)
        Mockito.`when`(user.uid).thenReturn(uid)
        Mockito.`when`(userRepository.currentUser).thenReturn(user)
    }

    private fun repo() = FavoritesRepository(
        favoriteDao = favoriteDao,
        cachedRecipeDao = cachedDao,
        analytics = analytics,
        userRepository = userRepository,
        syncRepository = syncRepository
    )

    private fun recipe(id: String) =
        Recipe(id = id, name = "Recipe $id", description = "", imageUrl = "", category = "")

    private fun favEntity(id: String) = FavoriteEntity(
        id = id, name = "Recipe $id", imageUrl = "", category = "",
        rating = 0f, prepTime = 0, cookTime = 0, difficulty = "MEDIUM"
    )

    // ── toggleFavorite ─────────────────────────────────────────────────────────────

    @Test
    fun `toggleFavorite adds, caches, logs, and uploads when not favorite and signed in`() = runTest {
        signInAs("uid1")

        repo().toggleFavorite(recipe("r1"), isCurrentlyFavorite = false)

        assertEquals(listOf("r1"), favoriteDao.items.value.map { it.id })
        assertEquals(listOf("r1"), cachedDao.upserts.map { it.id })
        verify(analytics).logRecipeFavorited("r1", "Recipe r1")
        // Concrete args (no matchers) — see note in toggleFavorite-removal test below.
        verify(syncRepository).uploadFavorite("uid1", favEntity("r1"))
    }

    @Test
    fun `toggleFavorite removes locally and remotely when currently favorite`() = runTest {
        signInAs("uid1")
        favoriteDao.insert(favEntity("r1"))

        repo().toggleFavorite(recipe("r1"), isCurrentlyFavorite = true)

        assertTrue(favoriteDao.items.value.isEmpty())
        verify(syncRepository).deleteFavorite("uid1", "r1")
        assertTrue("cache must not be touched on removal", cachedDao.upserts.isEmpty())
    }

    @Test
    fun `toggleFavorite skips sync when signed out`() = runTest {
        // userRepository.currentUser defaults to null (signed out).
        repo().toggleFavorite(recipe("r1"), isCurrentlyFavorite = false)

        assertEquals(listOf("r1"), favoriteDao.items.value.map { it.id })
        verifyNoInteractions(syncRepository)
    }

    // ── removeFavorite / addFavorite ───────────────────────────────────────────────

    @Test
    fun `removeFavorite deletes locally and remotely when signed in`() = runTest {
        signInAs("uid1")
        favoriteDao.insert(favEntity("r1"))

        repo().removeFavorite("r1")

        assertTrue(favoriteDao.items.value.isEmpty())
        verify(syncRepository).deleteFavorite("uid1", "r1")
    }

    @Test
    fun `addFavorite inserts the favorite without populating the recipe cache`() = runTest {
        repo().addFavorite(recipe("r1"))

        assertEquals(listOf("r1"), favoriteDao.items.value.map { it.id })
        assertTrue("addFavorite must not corrupt the detail cache", cachedDao.upserts.isEmpty())
    }

    // ── favorites flow ─────────────────────────────────────────────────────────────

    @Test
    fun `favorites flow maps stored entities to domain recipes`() = runTest {
        favoriteDao.insert(favEntity("r1"))
        favoriteDao.insert(favEntity("r2"))

        val favorites = repo().favorites.first()

        assertEquals(setOf("r1", "r2"), favorites.map { it.id }.toSet())
        assertEquals("Recipe r1", favorites.first { it.id == "r1" }.name)
    }
}
