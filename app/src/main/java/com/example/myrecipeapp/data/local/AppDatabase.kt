package com.example.myrecipeapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Single Room database for the app.
 *
 * Tables:
 *  - favorites      (FavoriteEntity)
 *  - shopping_items (ShoppingItemEntity)
 *
 * Version history:
 *  1 — initial schema (favorites + shopping_items)
 */
@Database(
    entities = [FavoriteEntity::class, ShoppingItemEntity::class],
    version = 1,
    exportSchema = false   // no migration tracking needed for v1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myrecipe_db"
                ).build().also { INSTANCE = it }
            }
    }
}
