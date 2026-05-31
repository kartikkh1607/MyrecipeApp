package com.kartik.mealtime.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartik.mealtime.domain.model.Recipe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for [AiRecipeDao] against a real (in-memory) Room database.
 *
 * Verifies the JSON TypeConverter round-trip plus the `ORDER BY createdAt DESC`
 * contract the AI Creations screen relies on for newest-first display.
 */
@RunWith(AndroidJUnit4::class)
class AiRecipeDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AiRecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.aiRecipeDao()
    }

    @After
    fun tearDown() = db.close()

    private fun entity(
        id: String,
        name: String = "Recipe $id",
        source: AiRecipeSource = AiRecipeSource.GENERATED,
        createdAt: Long,
    ) = AiRecipeEntity(
        id = id,
        recipe = Recipe(
            id = id, name = name, description = "", imageUrl = "", category = ""
        ),
        source = source.name,
        createdAt = createdAt,
    )

    @Test
    fun upsertedRecipeRoundTripsThroughTheJsonConverter() = runBlocking {
        dao.upsert(entity("1", name = "AI Pasta", createdAt = 1L))

        val loaded = dao.getById("1")

        assertNotNull(loaded)
        assertEquals("AI Pasta", loaded!!.recipe.name)
        assertEquals(AiRecipeSource.GENERATED.name, loaded.source)
    }

    @Test
    fun getAllFlowReturnsNewestFirst() = runBlocking {
        dao.upsert(entity("a", createdAt = 1L))
        dao.upsert(entity("b", createdAt = 3L))
        dao.upsert(entity("c", createdAt = 2L))

        val ids = dao.getAllFlow().first().map { it.id }

        assertEquals(listOf("b", "c", "a"), ids)
    }

    @Test
    fun upsertReplacesAnExistingRowWithTheSameId() = runBlocking {
        dao.upsert(entity("1", name = "Original", createdAt = 1L))
        dao.upsert(entity("1", name = "Updated", createdAt = 2L))

        val all = dao.getAllFlow().first()
        assertEquals(1, all.size)
        assertEquals("Updated", all.single().recipe.name)
    }

    @Test
    fun deleteRemovesOnlyTheMatchingRow() = runBlocking {
        dao.upsert(entity("1", createdAt = 1L))
        dao.upsert(entity("2", createdAt = 2L))

        dao.delete("1")

        assertEquals(listOf("2"), dao.getAllFlow().first().map { it.id })
    }

    @Test
    fun deleteAllEmptiesTheTable() = runBlocking {
        dao.upsert(entity("1", createdAt = 1L))
        dao.upsert(entity("2", createdAt = 2L))

        dao.deleteAll()

        assertTrue(dao.getAllFlow().first().isEmpty())
    }

    @Test
    fun getByIdReturnsNullForUnknownId() = runBlocking {
        assertNull(dao.getById("missing"))
    }
}
