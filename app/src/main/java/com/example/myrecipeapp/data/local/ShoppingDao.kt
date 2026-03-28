package com.example.myrecipeapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    /** Emits the full list in insertion order every time items change. */
    @Query("SELECT * FROM shopping_items ORDER BY rowid ASC")
    fun getAllFlow(): Flow<List<ShoppingItemEntity>>

    /**
     * Inserts a new item. IGNORE = silently skips duplicates
     * (same key = same recipe+ingredient already in the list).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET isChecked = :isChecked WHERE key = :key")
    suspend fun setChecked(key: String, isChecked: Boolean)

    @Query("DELETE FROM shopping_items WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM shopping_items WHERE isChecked = 1")
    suspend fun deleteChecked()

    @Query("DELETE FROM shopping_items")
    suspend fun deleteAll()
}
