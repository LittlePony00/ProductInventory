package com.android.rut.miit.productinventory.feature.household.data

import com.android.rut.miit.productinventory.core.local.HouseholdLocalDataSource
import com.android.rut.miit.productinventory.feature.household.api.models.Household
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class HouseholdRepositoryImplTest {

    @Test
    fun `cached household read does not require remote access`() = runTest {
        val local = FakeHouseholdLocalDataSource(listOf(household("local", "Local")))
        val repository = HouseholdRepositoryImpl(
            remoteDataSource = HouseholdRemoteDataSource(erroringHttpClient()),
            localDataSource = local
        )

        assertEquals(listOf("Local"), repository.getCachedHouseholds().map { it.name })
        assertEquals(listOf("Local"), repository.getMyHouseholds().map { it.name })
    }

    @Test
    fun `refresh loads remote households into local cache`() = runTest {
        val local = FakeHouseholdLocalDataSource(listOf(household("local", "Local")))
        val repository = HouseholdRepositoryImpl(
            remoteDataSource = HouseholdRemoteDataSource(
                httpClient(
                    """
                        [
                          {"id":"remote","name":"Remote","createdAt":"2026-05-15T00:00:00Z"}
                        ]
                    """.trimIndent()
                )
            ),
            localDataSource = local
        )

        assertEquals(listOf("Remote"), repository.refreshMyHouseholds().map { it.name })
        assertEquals(listOf("Remote"), local.getHouseholds().map { it.name })
    }

    private fun httpClient(response: String): HttpClient =
        HttpClient(
            MockEngine {
                respond(
                    content = response,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun erroringHttpClient(): HttpClient =
        HttpClient(
            MockEngine {
                respondError(HttpStatusCode.ServiceUnavailable)
            }
        ) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

    private fun household(id: String, name: String) =
        Household(id = id, name = name, createdAt = "2026-05-15T00:00:00Z")

    private class FakeHouseholdLocalDataSource(
        initialHouseholds: List<Household> = emptyList()
    ) : HouseholdLocalDataSource {
        private var households = initialHouseholds

        override suspend fun getHouseholds(): List<Household> = households

        override suspend fun saveHouseholds(households: List<Household>) {
            this.households = households
        }

        override suspend fun getHouseholdById(id: String): Household? =
            households.firstOrNull { it.id == id }
    }
}
