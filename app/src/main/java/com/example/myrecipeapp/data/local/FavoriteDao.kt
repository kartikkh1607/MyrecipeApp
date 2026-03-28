package com.example.myrecipeapp.data.local

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface FavoriteDao {

    /** Emits the full list every time favorites change. */
    @Query("SELECT * FROM favorites ORDER BY rowid DESC")
    fun getAllFlow(): Flow<List<FavoriteEntity>>

    /** Emits the set of favorited IDs — used to drive the heart icon state. */
    @Query("SELECT id FROM favorites")
    fun getIdsFlow(): Flow<List<String>>

    /** Inserts or replaces (handles re-adding a previously removed favorite). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun delete(id: String)
}
