package com.android.rut.miit.productinventory.infrastructure.config

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.core.io.ClassPathResource
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ScriptUtils

class ProductRemainingAmountMigrationTest {

    @Test
    fun `v3 backfills remaining amount and enforces not null constraint`() {
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:product_remaining_amount_migration_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )
        val jdbcTemplate = JdbcTemplate(dataSource)
        jdbcTemplate.execute(
            """
            CREATE TABLE products (
                id UUID PRIMARY KEY,
                quantity DOUBLE PRECISION NOT NULL,
                remaining_amount DOUBLE PRECISION
            )
            """.trimIndent()
        )
        val productId = UUID.randomUUID()
        jdbcTemplate.update(
            "INSERT INTO products (id, quantity, remaining_amount) VALUES (?, ?, NULL)",
            productId,
            5.0
        )

        dataSource.connection.use { connection ->
            ScriptUtils.executeSqlScript(
                connection,
                ClassPathResource("db/migration/V3__backfill_product_remaining_amount.sql")
            )
        }

        assertEquals(
            5.0,
            jdbcTemplate.queryForObject(
                "SELECT remaining_amount FROM products WHERE id = ?",
                Double::class.java,
                productId
            )
        )
        assertFailsWith<DataAccessException> {
            jdbcTemplate.update(
                "INSERT INTO products (id, quantity, remaining_amount) VALUES (?, ?, NULL)",
                UUID.randomUUID(),
                1.0
            )
        }
    }

    @Test
    fun `v4 removes local database rows from global barcode cache`() {
        val dataSource = DriverManagerDataSource(
            "jdbc:h2:mem:barcode_cache_cleanup_migration_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )
        val jdbcTemplate = JdbcTemplate(dataSource)
        jdbcTemplate.execute(
            """
            CREATE TABLE barcode_product_cache (
                barcode varchar(255) primary key,
                source varchar(255) not null
            )
            """.trimIndent()
        )
        jdbcTemplate.update(
            "INSERT INTO barcode_product_cache (barcode, source) VALUES (?, ?)",
            "local",
            "LOCAL_DATABASE"
        )
        jdbcTemplate.update(
            "INSERT INTO barcode_product_cache (barcode, source) VALUES (?, ?)",
            "external",
            "OPEN_FOOD_FACTS"
        )

        dataSource.connection.use { connection ->
            ScriptUtils.executeSqlScript(
                connection,
                ClassPathResource("db/migration/V4__remove_local_database_barcode_cache.sql")
            )
        }

        assertEquals(
            0,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM barcode_product_cache WHERE source = 'LOCAL_DATABASE'",
                Int::class.java
            )
        )
        assertEquals(
            1,
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM barcode_product_cache WHERE source = 'OPEN_FOOD_FACTS'",
                Int::class.java
            )
        )
    }
}
