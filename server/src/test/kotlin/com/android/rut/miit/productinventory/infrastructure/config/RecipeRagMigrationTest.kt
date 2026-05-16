package com.android.rut.miit.productinventory.infrastructure.config

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import org.flywaydb.core.Flyway
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource

class RecipeRagMigrationTest {

    @Test
    fun `creates and seeds recipe documents table`() {
        val datasource = DriverManagerDataSource(
            "jdbc:h2:mem:recipe_rag_migration_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )

        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        val jdbcTemplate = JdbcTemplate(datasource)

        assertEquals(
            5,
            jdbcTemplate.queryForObject("select count(*) from recipe_documents", Int::class.java)
        )
        assertEquals(
            "Овощная рисовая боул-тарелка",
            jdbcTemplate.queryForObject(
                "select title from recipe_documents where id = 'vegetable-rice-bowl'",
                String::class.java
            )
        )
    }

    @Test
    fun `localizes existing recipe documents on upgrade`() {
        val datasource = DriverManagerDataSource(
            "jdbc:h2:mem:recipe_rag_upgrade_${UUID.randomUUID()};DB_CLOSE_DELAY=-1",
            "sa",
            ""
        )

        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration")
            .target("8")
            .load()
            .migrate()

        val jdbcTemplate = JdbcTemplate(datasource)
        assertEquals(
            "Vegetable Rice Bowl",
            jdbcTemplate.queryForObject(
                "select title from recipe_documents where id = 'vegetable-rice-bowl'",
                String::class.java
            )
        )

        Flyway.configure()
            .dataSource(datasource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        assertEquals(
            "Овощная рисовая боул-тарелка",
            jdbcTemplate.queryForObject(
                "select title from recipe_documents where id = 'vegetable-rice-bowl'",
                String::class.java
            )
        )
    }
}
