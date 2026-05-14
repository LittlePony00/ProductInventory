package com.android.rut.miit.productinventory.infrastructure.config

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class ProductSchemaMigration(
    private val jdbcTemplate: JdbcTemplate
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        jdbcTemplate.update(BACKFILL_REMAINING_AMOUNT_SQL)
        jdbcTemplate.execute(REQUIRE_REMAINING_AMOUNT_SQL)
    }

    companion object {
        const val BACKFILL_REMAINING_AMOUNT_SQL =
            "UPDATE products SET remaining_amount = quantity WHERE remaining_amount IS NULL"

        const val REQUIRE_REMAINING_AMOUNT_SQL =
            "ALTER TABLE products ALTER COLUMN remaining_amount SET NOT NULL"
    }
}
