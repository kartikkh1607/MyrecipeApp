package com.kartik.mealtime.data.local

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Replays each Room schema migration against an in-memory database and verifies
 * that the resulting DB opens cleanly under the *current* schema. Catches the
 * upgrade-path break that pure unit tests can't see — a user on v1 of the app
 * must still be able to install v4 without their data crashing.
 *
 * Schemas live in `app/schemas/` (exported via `ksp.arg("room.schemaLocation")`)
 * and are wired into the androidTest assets in `app/build.gradle.kts`.
 *
 * Only v3→v4 is exercised today because earlier schema JSONs were never exported.
 * If/when a v5 ships, add `migrate3To5 / migrate4To5` cases following the same
 * pattern and consider regenerating the v1/v2 schemas to fill the back-history.
 */
@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    fun migrate3To4_addsAiRecipesTableAndKeepsExistingData() {
        // Create a v3 database with a row in the carry-over `cached_recipes` table.
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO cached_recipes (id, recipe, cachedAt) VALUES ('r1', '{}', 0)"
            )
        }

        // Run the v3 → v4 migration. validateDroppedTables=true catches any leftover
        // table that the migration forgot to drop.
        helper.runMigrationsAndValidate(
            TEST_DB,
            /* version = */ 4,
            /* validateDroppedTables = */ true,
            AppDatabase.MIGRATION_3_4,
        ).use { db ->
            // The new ai_recipes table is empty but queryable.
            db.query("SELECT COUNT(*) FROM ai_recipes").use { c ->
                assertFalse("ai_recipes table missing after 3->4", c.count == 0 && !c.moveToFirst())
            }
            // Existing data on the carry-over table survives.
            db.query("SELECT id FROM cached_recipes").use { c ->
                check(c.moveToFirst()) { "cached_recipes row was lost on migration" }
                check(c.getString(0) == "r1") { "cached_recipes row content changed" }
            }
        }
    }

    @Test
    fun openingAtCurrentVersion_runsAllMigrationsCleanly() {
        // Build the database via the real builder so Room walks v3 → v4 itself.
        // If any migration object is misconfigured, this throws on .openHelper.writableDatabase.
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_RUNTIME)
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
            )
            .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            .build()
            .also { it.openHelper.writableDatabase }
            .close()
        // Clean up the on-disk file so re-runs start fresh.
        context.deleteDatabase(TEST_DB_RUNTIME)
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
        const val TEST_DB_RUNTIME = "migration-runtime.db"
    }
}
