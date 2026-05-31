package com.kartik.mealtime.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.kartik.mealtime.domain.model.FeaturedType
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
 * Instrumented test for [FeaturedCacheDao] against a real (in-memory) Room database.
 *
 * The table is single-row (PK hard-coded to 0) — verifies that upsert overwrites
 * the existing payload rather than accumulating duplicate cache entries.
 */
@RunWith(AndroidJUnit4::class)
class FeaturedCacheDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FeaturedCacheDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.featuredCacheDao()
    }

    @After
    fun tearDown() = db.close()

    private fun featured(id: String, name: String = "Recipe $id") = FeaturedRecipe(
        recipe = Recipe(
            id = id, name = name, description = "", imageUrl = "", category = ""
        ),
        type = FeaturedType.RECIPE_OF_THE_DAY,
    )

    @Test
    fun getReturnsNullWhenTheTableIsEmpty() = runBlocking {
        assertNull(dao.get())
    }

    @Test
    fun upsertedPayloadRoundTrips() = runBlocking {
        val list = listOf(featured("1"), featured("2"))
        dao.upsert(FeaturedCacheEntity.fromList(list))

        val loaded = dao.get()
        assertNotNull(loaded)
        val decoded = FeaturedCacheEntity.toList(loaded!!)
        assertEquals(listOf("1", "2"), decoded.map { it.recipe.id })
    }

    @Test
    fun upsertOverwritesTheSingleCachedRow() = runBlocking {
        dao.upsert(FeaturedCacheEntity.fromList(listOf(featured("old"))))
        dao.upsert(FeaturedCacheEntity.fromList(listOf(featured("new"))))

        val decoded = FeaturedCacheEntity.toList(dao.get()!!)
        assertEquals(listOf("new"), decoded.map { it.recipe.id })
    }
}
