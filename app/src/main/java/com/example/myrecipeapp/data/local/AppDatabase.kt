package com.example.myrecipeapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single Room database for the app.
 *
 * Tables:
 *  - favorites      (FavoriteEntity)
 *  - shopping_items (ShoppingItemEntity)
 *  - cached_recipes (CachedRecipeEntity) — full recipe detail cache for offline access
 *
 * Version history:
 *  1 — initial schema (favorites + shopping_items)
 *  2 — added cached_recipes table for offline recipe detail (Issue #7)
 */
@Database(
    entities = [FavoriteEntity::class, ShoppingItemEntity::class, CachedRecipeEntity::class],
    version = 2,
    exportSchema = true   // schema history tracked in app/schemas/
)
@TypeConverters(RecipeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun cachedRecipeDao(): CachedRecipeDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Adds the `cached_recipes` table that was introduced in version 2.
         * The columns match [CachedRecipeEntity] exactly.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_recipes` (
                        `id`       TEXT    NOT NULL PRIMARY KEY,
                        `recipe`   TEXT    NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myrecipe_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
