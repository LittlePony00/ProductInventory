package com.android.rut.miit.productinventory.infrastructure.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.springframework.boot.DefaultApplicationArguments
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.util.UUID

class ProductSchemaMigrationTest {

    @Test
    fun `backfills remaining amount and enforces not null constraint`() {
        val jdbcTemplate = JdbcTemplate(
            DriverManagerDataSource(
                "jdbc:h2:mem:product_schema_migration_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
                "sa",
                ""
            )
        )
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

        ProductSchemaMigration(jdbcTemplate).run(DefaultApplicationArguments())

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
}
