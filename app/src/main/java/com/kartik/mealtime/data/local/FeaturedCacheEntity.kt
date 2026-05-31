package com.kartik.mealtime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kartik.mealtime.domain.model.FeaturedRecipe
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Single-row cache of the home-screen featured recipes.
 *
 * `id` is hard-coded to 0 — there's exactly one row, overwritten on every successful
 * fetch. Storing the list as a JSON blob keeps the table schema trivial (no separate
 * table for items, no joins) at the cost of a kotlinx.serialization round-trip on
 * read/write — handled by [FeaturedCacheSerializer] below.
 *
 * The 12-hour TTL is set in [isExpired]; the random-recipes endpoint costs 5 API
 * points per call, so caching is the biggest single quota saving.
 */
@Entity(tableName = "featured_cache")
data class FeaturedCacheEntity(
    @PrimaryKey val id: Int = 0,
    val payload: String,                  // kotlinx.serialization JSON of List<FeaturedRecipe>
    val cachedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(): Boolean =
        System.currentTimeMillis() - cachedAt > CACHE_MAX_AGE_MS

    companion object {
        const val CACHE_MAX_AGE_MS = 12 * 60 * 60 * 1_000L   // 12 hours

        // Serialization helpers were moved out of the companion (see
        // FeaturedCacheSerializer) because Room's KSP processor inspects an
        // entity's companion for embedded/relation annotations, and tripped on
        // the kotlinx.serialization-generated `FeaturedRecipe.serializer()`
        // reference before the serialization plugin had produced it.
        fun fromList(list: List<FeaturedRecipe>): FeaturedCacheEntity =
            FeaturedCacheEntity(payload = FeaturedCacheSerializer.encode(list))

        fun toList(entity: FeaturedCacheEntity): List<FeaturedRecipe> =
            FeaturedCacheSerializer.decode(entity.payload)
    }
}

private object FeaturedCacheSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }
    private val listSerializer = ListSerializer(FeaturedRecipe.serializer())

    fun encode(list: List<FeaturedRecipe>): String =
        json.encodeToString(listSerializer, list)

    fun decode(payload: String): List<FeaturedRecipe> =
        json.decodeFromString(listSerializer, payload)
}
