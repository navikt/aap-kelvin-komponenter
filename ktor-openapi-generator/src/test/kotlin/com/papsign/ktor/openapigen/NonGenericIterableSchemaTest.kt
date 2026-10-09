package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

internal class NonGenericIterableSchemaTest {

    private data class ResponseWithPeriod(val period: NonGenericPeriod)

    private data class NonGenericPeriod(val start: LocalDate, val end: LocalDate) : Iterable<LocalDate> {
        override fun iterator(): Iterator<LocalDate> = listOf(start, end).iterator()
    }

    @Test
    fun `response containing non-generic Iterable can generate OpenAPI specification`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test-iterable-period") {
                    get<Unit, ResponseWithPeriod> {
                        respond(
                            ResponseWithPeriod(
                                NonGenericPeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 14))
                            )
                        )
                    }
                }
            }
        }

        val response = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, response.status)
        val specification = ObjectMapper().readTree(response.bodyAsText())
        assertThat(specification.at("/components/schemas/ResponseWithPeriod/properties/period/\$ref").asText())
            .isEqualTo("#/components/schemas/NonGenericPeriod")
        val periodSchema = specification.at("/components/schemas/NonGenericPeriod")
        assertThat(periodSchema.path("type").asText()).isEqualTo("object")
        assertThat(periodSchema.path("properties").fieldNames().asSequence().toList())
            .containsExactlyInAnyOrder("start", "end")
        for (property in listOf("start", "end")) {
            assertThat(periodSchema.at("/properties/$property/type").asText()).isEqualTo("string")
            assertThat(periodSchema.at("/properties/$property/format").asText()).isEqualTo("date")
        }

        val endpointResponse = client.get("/test-iterable-period")
        assertEquals(HttpStatusCode.OK, endpointResponse.status)
        val objectMapper = ObjectMapper()
        assertThat(objectMapper.readTree(endpointResponse.bodyAsText())).isEqualTo(
            objectMapper.readTree("""{"period":{"start":"2025-01-01","end":"2025-01-14"}}""")
        )
    }
}
