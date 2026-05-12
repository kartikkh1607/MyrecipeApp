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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedRecipeEntity)

    @Query("SELECT * FROM cached_recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedRecipeEntity?

    @Query("SELECT COUNT(*) FROM cached_recipes")
    suspend fun count(): Int

    @Query("DELETE FROM cached_recipes WHERE id IN (SELECT id FROM cached_recipes ORDER BY cachedAt ASC LIMIT :count)")
    suspend fun evictOldest(count: Int)

    @Query("DELETE FROM cached_recipes")
    suspend fun deleteAll()
}
