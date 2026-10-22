package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse

internal class ValueClassRouteTest {

    @JvmInline
    value class VedtakId(val id: Long)

    @JvmInline
    value class WrappedValue<T>(val value: T) {
        val text: String get() = value.toString()
    }

    data class Vedtak(val id: VedtakId, val name: WrappedValue<String>, val ids: List<VedtakId>)

    @ParameterizedTest
    @ValueSource(longs = [Long.MIN_VALUE, 0, 42, Long.MAX_VALUE])
    fun `value class round trips and is documented as its underlying number`(id: Long) = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("vedtak") {
                    post<Unit, VedtakId, VedtakId> { _, body ->
                        assertEquals(VedtakId(id), body)
                        respond(body)
                    }
                }
            }
        }

        val response = client.post("/vedtak") {
            contentType(ContentType.Application.Json)
            setBody(id.toString())
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(id.toString(), response.bodyAsText())

        val openApiResponse = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, openApiResponse.status)
        val specification = ObjectMapper().readTree(openApiResponse.bodyAsText())
        val operation = specification.at("/paths/~1vedtak/post")
        val schemaRef = "#/components/schemas/VedtakId"
        assertEquals(
            schemaRef,
            operation.at("/requestBody/content/application~1json/schema/\$ref").asText()
        )
        assertEquals(
            schemaRef,
            operation.at("/responses/200/content/application~1json/schema/\$ref").asText()
        )

        val schema = specification.at("/components/schemas/VedtakId")
        assertEquals("integer", schema.path("type").asText())
        assertEquals("int64", schema.path("format").asText())
        assertFalse(schema.has("properties"))
        assertFalse(schema.has("required"))
    }

    @Test
    fun `value classes in properties and lists use underlying types including generic values`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("vedtak") {
                    post<Unit, Vedtak, Vedtak> { _, body ->
                        assertEquals(Vedtak(VedtakId(42), WrappedValue("name"), listOf(VedtakId(43))), body)
                        respond(body)
                    }
                }
            }
        }

        val json = """{"id":42,"name":"name","ids":[43]}"""
        val response = client.post("/vedtak") {
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val mapper = ObjectMapper()
        assertEquals(mapper.readTree(json), mapper.readTree(response.bodyAsText()))

        val openApiResponse = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, openApiResponse.status)
        val specification = mapper.readTree(openApiResponse.bodyAsText())
        val properties = specification.at("/components/schemas/Vedtak/properties")
        assertEquals("#/components/schemas/VedtakId", properties.at("/id/\$ref").asText())
        assertEquals("array", properties.at("/ids/type").asText())
        assertEquals("#/components/schemas/VedtakId", properties.at("/ids/items/\$ref").asText())
        assertEquals("integer", specification.at("/components/schemas/VedtakId/type").asText())
        val nameSchema = specification.at(properties.at("/name/\$ref").asText().removePrefix("#"))
        assertEquals("string", nameSchema.path("type").asText())
        assertFalse(nameSchema.has("properties"))
    }
}
