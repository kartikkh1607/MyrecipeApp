package com.kartik.mealtime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartik.mealtime.domain.model.FeaturedRecipe
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Single-row cache of the home-screen featured recipes.
 *
 * `id` is hard-coded to 0 — there's exactly one row, overwritten on every successful
 * fetch. Storing the list as a JSON blob keeps the table schema trivial (no separate
 * table for items, no joins) at the cost of a Gson round-trip on read/write.
 *
 * The 12-hour TTL is set in [isExpired]; the random-recipes endpoint costs 5 API
 * points per call, so caching is the biggest single quota saving.
 */
@Entity(tableName = "featured_cache")
data class FeaturedCacheEntity(
    @PrimaryKey val id: Int = 0,
    val payload: String,                  // Gson JSON of List<FeaturedRecipe>
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean =
        System.currentTimeMillis() - cachedAt > CACHE_MAX_AGE_MS

    companion object {
        const val CACHE_MAX_AGE_MS = 12 * 60 * 60 * 1_000L   // 12 hours
        private val gson = Gson()
        private val listType = object : TypeToken<List<FeaturedRecipe>>() {}.type

        fun fromList(list: List<FeaturedRecipe>): FeaturedCacheEntity =
            FeaturedCacheEntity(payload = gson.toJson(list, listType))

        fun toList(entity: FeaturedCacheEntity): List<FeaturedRecipe> =
            gson.fromJson(entity.payload, listType)
    }
}
