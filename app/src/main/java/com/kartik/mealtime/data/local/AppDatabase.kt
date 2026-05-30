package com.kartik.mealtime.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Single Room database for the app.
 *
 * Tables:
 *  - favorites       (FavoriteEntity)
 *  - shopping_items  (ShoppingItemEntity)
 *  - cached_recipes  (CachedRecipeEntity) — full recipe detail cache for offline access
 *  - featured_cache  (FeaturedCacheEntity) — single-row JSON cache of home featured list
 *
 * Version history:
 *  1 — initial schema (favorites + shopping_items)
 *  2 — added cached_recipes table for offline recipe detail (Issue #7)
 *  3 — added featured_cache table to skip the 5-pt random-recipes API call
 *  4 — added ai_recipes table for the user's AI Creations collection
 */
@Database(
    entities = [
        FavoriteEntity::class,
        ShoppingItemEntity::class,
        CachedRecipeEntity::class,
        FeaturedCacheEntity::class,
        AiRecipeEntity::class
    ],
    version = 4,
    exportSchema = true   // schema history tracked in app/schemas/
)
@TypeConverters(RecipeTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoriteDao(): FavoriteDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun cachedRecipeDao(): CachedRecipeDao
    abstract fun featuredCacheDao(): FeaturedCacheDao
    abstract fun aiRecipeDao(): AiRecipeDao

    companion object {
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

        /**
         * Adds the `featured_cache` table for persistent featured-recipes caching (v3).
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `featured_cache` (
                        `id`       INTEGER NOT NULL PRIMARY KEY,
                        `payload`  TEXT    NOT NULL,
                        `cachedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds the `ai_recipes` table for the user's AI Creations (v4).
         * Columns match [AiRecipeEntity] exactly; `recipe` holds the JSON blob.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_recipes` (
                        `id`             TEXT    NOT NULL PRIMARY KEY,
                        `recipe`         TEXT    NOT NULL,
                        `source`         TEXT    NOT NULL,
                        `sourceRecipeId` TEXT,
                        `createdAt`      INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        const val DB_NAME = "myrecipe_db"
    }
}
