package com.kartik.mealtime.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [FavoriteDao] against a real (in-memory) Room database.
 *
 * Exercises the actual SQLite-backed DAO — query ordering, INSERT-OR-REPLACE
 * conflict handling, and deletes — which the unit-test fakes only approximate.
 * Requires a connected device/emulator: run with
 * `./gradlew.bat :app:connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavoriteDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.favoriteDao()
    }

    @After
    fun tearDown() = db.close()

    private fun fav(id: String, name: String = "Recipe $id") = FavoriteEntity(
        id = id, name = name, imageUrl = "", category = "",
        rating = 0f, prepTime = 0, cookTime = 0, difficulty = "MEDIUM"
    )

    @Test
    fun insertedFavoritesAreReturnedByTheFlow() = runBlocking {
        dao.insert(fav("1"))
        dao.insert(fav("2"))

        val all = dao.getAllFlow().first()

        assertEquals(setOf("1", "2"), all.map { it.id }.toSet())
    }

    @Test
    fun insertReplacesAnExistingFavoriteWithTheSameId() = runBlocking {
        dao.insert(fav("1", name = "Original"))
        dao.insert(fav("1", name = "Updated"))

        val all = dao.getAllFlow().first()

        assertEquals(1, all.size)
        assertEquals("Updated", all.single().name)
    }

    @Test
    fun deleteRemovesOnlyTheMatchingFavorite() = runBlocking {
        dao.insert(fav("1"))
        dao.insert(fav("2"))

        dao.delete("1")

        assertEquals(listOf("2"), dao.getAllFlow().first().map { it.id })
    }

    @Test
    fun deleteAllEmptiesTheTable() = runBlocking {
        dao.insert(fav("1"))
        dao.insert(fav("2"))

        dao.deleteAll()

        assertTrue(dao.getAllFlow().first().isEmpty())
    }

    @Test
    fun getAllSyncReturnsTheCurrentSnapshot() = runBlocking {
        dao.insert(fav("1"))

        assertEquals(listOf("1"), dao.getAllSync().map { it.id })
    }
}
