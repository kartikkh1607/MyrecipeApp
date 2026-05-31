package com.kartik.mealtime.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [CachedRecipeDao] against a real (in-memory) Room database.
 *
 * Exercises the JSON TypeConverter round-trip (Recipe ↔ TEXT) and the
 * `evictOldest` window function that the offline-detail cache depends on.
 */
@RunWith(AndroidJUnit4::class)
class CachedRecipeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: CachedRecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.cachedRecipeDao()
    }

    @After
    fun tearDown() = db.close()

    private fun recipe(id: String, name: String = "Recipe $id") = Recipe(
        id = id,
        name = name,
        description = "",
        imageUrl = "",
        category = "",
    )

    private fun entity(id: String, name: String = "Recipe $id", cachedAt: Long) =
        CachedRecipeEntity(id = id, recipe = recipe(id, name), cachedAt = cachedAt)

    @Test
    fun upsertedRecipeRoundTripsThroughTheJsonConverter() = runBlocking {
        dao.upsert(entity("1", name = "Pad Thai", cachedAt = 1L))

        val loaded = dao.getById("1")

        assertNotNull(loaded)
        assertEquals("1", loaded!!.id)
        assertEquals("Pad Thai", loaded.recipe.name)
        assertEquals(1L, loaded.cachedAt)
    }

    @Test
    fun upsertReplacesAnExistingRowWithTheSameId() = runBlocking {
        dao.upsert(entity("1", name = "Original", cachedAt = 1L))
        dao.upsert(entity("1", name = "Updated", cachedAt = 2L))

        assertEquals(1, dao.count())
        assertEquals("Updated", dao.getById("1")!!.recipe.name)
    }

    @Test
    fun getByIdReturnsNullForUnknownId() = runBlocking {
        assertNull(dao.getById("missing"))
    }

    @Test
    fun countReflectsTheNumberOfRows() = runBlocking {
        assertEquals(0, dao.count())
        dao.upsert(entity("1", cachedAt = 1L))
        dao.upsert(entity("2", cachedAt = 2L))
        assertEquals(2, dao.count())
    }

    @Test
    fun evictOldestRemovesTheNRowsWithSmallestCachedAt() = runBlocking {
        dao.upsert(entity("oldest", cachedAt = 1L))
        dao.upsert(entity("middle", cachedAt = 2L))
        dao.upsert(entity("newest", cachedAt = 3L))

        dao.evictOldest(2)

        assertNull(dao.getById("oldest"))
        assertNull(dao.getById("middle"))
        assertNotNull(dao.getById("newest"))
    }

    @Test
    fun deleteAllEmptiesTheTable() = runBlocking {
        dao.upsert(entity("1", cachedAt = 1L))
        dao.upsert(entity("2", cachedAt = 2L))

        dao.deleteAll()

        assertEquals(0, dao.count())
    }
}
