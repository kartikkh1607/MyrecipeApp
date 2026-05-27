package com.kartik.mealtime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the user's "AI Creations" ([AiRecipeEntity]). Newest first so the
 * collection screen shows the latest creation at the top.
 */
@Dao
interface AiRecipeDao {

    @Query("SELECT * FROM ai_recipes ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<AiRecipeEntity>>

    @Query("SELECT * FROM ai_recipes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AiRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AiRecipeEntity)

    @Query("DELETE FROM ai_recipes WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM ai_recipes")
    suspend fun deleteAll()
}
