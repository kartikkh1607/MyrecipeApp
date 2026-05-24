package com.kartik.mealtime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FeaturedCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FeaturedCacheEntity)

    @Query("SELECT * FROM featured_cache WHERE id = 0 LIMIT 1")
    suspend fun get(): FeaturedCacheEntity?
}
