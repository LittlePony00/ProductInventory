package com.android.rut.miit.productinventory.infrastructure.config

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FlywayMigrationFilenameTest {

    @Test
    fun `versioned flyway migrations have valid unique versions`() {
        val migrations = Files.list(Path.of("src/main/resources/db/migration"))
            .use { paths ->
                paths
                    .filter { Files.isRegularFile(it) }
                    .map { it.name }
                    .filter { it.endsWith(".sql") }
                    .map(::parseMigration)
                    .toList()
            }

        assertTrue(migrations.isNotEmpty(), "Expected at least one Flyway migration")
        assertEquals(
            migrations.map { it.version },
            migrations.map { it.version }.distinct(),
            "Flyway migration versions must be unique"
        )
    }

    private fun parseMigration(filename: String): MigrationFile {
        val match = migrationPattern.matchEntire(filename)
        requireNotNull(match) { "Invalid Flyway migration filename: $filename" }
        return MigrationFile(version = match.groupValues[1].toInt(), filename = filename)
    }

    private data class MigrationFile(
        val version: Int,
        val filename: String
    )

    private companion object {
        val migrationPattern = Regex("""V(\d+)__[a-z0-9_]+\.sql""")
    }
}
