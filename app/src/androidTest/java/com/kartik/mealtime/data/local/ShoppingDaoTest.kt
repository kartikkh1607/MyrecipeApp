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
 * Instrumented test for [ShoppingDao] against a real (in-memory) Room database.
 *
 * Verifies INSERT-OR-IGNORE dedup, the partial `deleteChecked` query, and
 * boolean column round-tripping that the unit-test fake can't fully exercise.
 */
@RunWith(AndroidJUnit4::class)
class ShoppingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ShoppingDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.shoppingDao()
    }

    @After
    fun tearDown() = db.close()

    private fun item(
        key: String,
        ingredient: String = "Ingredient $key",
        checked: Boolean = false,
    ) = ShoppingItemEntity(
        key = key,
        ingredientName = ingredient,
        amount = "1",
        unit = "cup",
        recipeName = "Recipe",
        checked = checked,
    )

    @Test
    fun insertedItemsAreReturnedByTheFlow() = runBlocking {
        dao.insert(item("a"))
        dao.insert(item("b"))

        val all = dao.getAllFlow().first()

        assertEquals(setOf("a", "b"), all.map { it.key }.toSet())
    }

    @Test
    fun insertWithDuplicateKeyIsIgnoredAndOriginalIsKept() = runBlocking {
        dao.insert(item("a", ingredient = "Original"))
        dao.insert(item("a", ingredient = "Duplicate"))

        val all = dao.getAllFlow().first()

        assertEquals(1, all.size)
        assertEquals("Original", all.single().ingredientName)
    }

    @Test
    fun setCheckedFlipsTheCheckedFlag() = runBlocking {
        dao.insert(item("a", checked = false))

        dao.setChecked("a", true)

        assertTrue(dao.getAllFlow().first().single().checked)
    }

    @Test
    fun deleteByKeyRemovesOnlyTheMatchingRow() = runBlocking {
        dao.insert(item("a"))
        dao.insert(item("b"))

        dao.deleteByKey("a")

        assertEquals(listOf("b"), dao.getAllFlow().first().map { it.key })
    }

    @Test
    fun deleteCheckedRemovesOnlyCheckedRows() = runBlocking {
        dao.insert(item("a", checked = false))
        dao.insert(item("b", checked = true))
        dao.insert(item("c", checked = true))

        dao.deleteChecked()

        assertEquals(listOf("a"), dao.getAllFlow().first().map { it.key })
    }

    @Test
    fun deleteAllEmptiesTheTable() = runBlocking {
        dao.insert(item("a"))
        dao.insert(item("b"))

        dao.deleteAll()

        assertTrue(dao.getAllFlow().first().isEmpty())
    }
}
