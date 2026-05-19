package com.android.rut.miit.productinventory.data.local

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.android.rut.miit.productinventory.di.MIGRATION_2_3
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AppDatabaseMigrationTest {

    @Test
    fun migration2To3PreservesProductsAndAddsCategoryColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(DB_NAME)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DB_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(2) {
                        override fun onCreate(db: SupportSQLiteDatabase) = Unit
                        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                    }
                )
                .build()
        )

        helper.writableDatabase.use { db ->
            db.createVersion2ProductsTable()
            db.execSQL(
                """
                INSERT INTO products (
                    id, name, brand, barcode, category, quantity, quantityUnit,
                    packageAmount, packageUnit, ingredientsText, calories, protein, fat, carbs,
                    purchaseDate, remainingAmount, lowStockThreshold, expirationDate,
                    expirationStatus, householdId, addedByUserId, createdAt, isPendingSync
                ) VALUES (
                    'p1', 'Milk', 'Brand', '4601234567890', 'DAIRY', 2.0, 'PIECES',
                    1.0, 'PIECES', 'milk', 42.0, 3.0, 2.0, 5.0,
                    '2026-05-14', 1.5, 0.5, '2026-05-20',
                    'FRESH', 'h1', 'u1', '2026-05-14T00:00:00Z', 1
                )
                """.trimIndent()
            )
            MIGRATION_2_3.migrate(db)

            val columns = db.productColumns()
            assertTrue("categoryId" in columns)
            assertTrue("categoryName" in columns)
            assertTrue("isPendingSync" in columns)
            db.query("SELECT name, category, remainingAmount, householdId FROM products WHERE id = 'p1'").use {
                assertTrue(it.moveToFirst())
                assertEquals("Milk", it.getString(0))
                assertEquals("DAIRY", it.getString(1))
                assertEquals(1.5, it.getDouble(2))
                assertEquals("h1", it.getString(3))
            }
        }
        helper.close()
        context.deleteDatabase(DB_NAME)
    }

    private fun SupportSQLiteDatabase.createVersion2ProductsTable() {
        execSQL(
            """
            CREATE TABLE IF NOT EXISTS products (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                brand TEXT,
                barcode TEXT,
                category TEXT NOT NULL,
                quantity REAL NOT NULL,
                quantityUnit TEXT NOT NULL,
                packageAmount REAL,
                packageUnit TEXT,
                ingredientsText TEXT,
                calories REAL,
                protein REAL,
                fat REAL,
                carbs REAL,
                purchaseDate TEXT,
                remainingAmount REAL NOT NULL,
                lowStockThreshold REAL,
                expirationDate TEXT,
                expirationStatus TEXT NOT NULL,
                householdId TEXT NOT NULL,
                addedByUserId TEXT NOT NULL,
                createdAt TEXT NOT NULL,
                isPendingSync INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun SupportSQLiteDatabase.productColumns(): Set<String> {
        val columns = mutableSetOf<String>()
        query("PRAGMA table_info(products)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                columns += cursor.getString(nameIndex)
            }
        }
        return columns
    }

    private companion object {
        const val DB_NAME = "migration-2-3-test"
    }
}
