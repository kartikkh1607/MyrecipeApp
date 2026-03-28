package com.example.myrecipeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface FavoriteDao {

    /** Emits the full list every time favorites change. */
    @Query("SELECT * FROM favorites ORDER BY rowid DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    /** Inserts or replaces (handles re-adding a previously removed favorite). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)
}
