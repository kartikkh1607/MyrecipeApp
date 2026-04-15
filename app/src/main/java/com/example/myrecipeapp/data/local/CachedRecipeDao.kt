package com.example.myrecipeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO for the [CachedRecipeEntity] table.
 *
 * Upsert semantics (INSERT OR REPLACE) mean the cache is always kept fresh:
 * every successful API detail fetch overwrites the previous snapshot.
 */
@Dao
interface CachedRecipeDao {

    /** Inserts or replaces a cached recipe. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedRecipeEntity)

    /** Returns the cached entity for [id], or null if not yet cached. */
    @Query("SELECT * FROM cached_recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedRecipeEntity?
}
