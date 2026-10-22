package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.model.schema.SchemaModel
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
import kotlin.test.assertTrue

@Suppress("DEPRECATION")
internal class ValueClassRouteTest {

    @JvmInline
    value class VedtakId(val id: Long)

    @JvmInline
    value class WrappedValue<T>(val value: T) {
        val text: String get() = value.toString()
    }

    data class Vedtak(val id: VedtakId, val name: WrappedValue<String>, val ids: List<VedtakId>)

    data class Payload(val text: String)

    @Deprecated("Use Payload directly")
    @JvmInline
    value class LegacyPayload(val value: Payload)

    @Deprecated("Use VedtakId directly")
    @JvmInline
    value class LegacyId(val value: VedtakId)

    @JvmInline
    value class AnnotatedPayload(
        @property:Description("Backing property description")
        @Deprecated("Backing property deprecation")
        val value: Payload
    )

    data class MetadataUses(
        val aPlainPayload: Payload,
        val bLegacyPayload: LegacyPayload,
        val cPlainPayload: Payload,
        val dPlainId: VedtakId,
        val eLegacyId: LegacyId,
        val fPlainId: VedtakId,
        val gAnnotatedPayload: AnnotatedPayload,
        @property:Description("Usage description")
        val hAnnotatedPayload: AnnotatedPayload
    )

    @Test
    fun `reference composition preserves metadata without mutating the reference`() {
        val ref = SchemaModel.SchemaModelRef<String>("#/components/schemas/Payload").apply {
            example = "example"
            examples = listOf("first", "second")
            description = "description"
            deprecated = true
        }

        val schema = ref.asSchemaObject()
        assertEquals(ref.example, schema.example)
        assertEquals(ref.examples, schema.examples)
        assertEquals(ref.description, schema.description)
        assertEquals(ref.deprecated, schema.deprecated)
        assertEquals(
            mapOf("\$ref" to "#/components/schemas/Payload"),
            schema.allOf.single().serialize()
        )
        schema.description = "changed"
        schema.deprecated = false
        assertEquals("description", ref.description)
        assertEquals(true, ref.deprecated)
    }

    @Test
    fun `wrapper annotations do not leak and backing property metadata is preserved`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("metadata") {
                    post<Unit, MetadataUses, MetadataUses> { _, body -> respond(body) }
                }
            }
        }

        val response = client.get("/openapi.json")
        assertEquals(HttpStatusCode.OK, response.status)
        val schemas = ObjectMapper().readTree(response.bodyAsText()).at("/components/schemas")
        val properties = schemas.at("/MetadataUses/properties")
        for (property in listOf("aPlainPayload", "cPlainPayload", "dPlainId", "fPlainId")) {
            assertFalse(properties.path(property).has("deprecated"), property)
            assertFalse(properties.path(property).has("description"), property)
        }
        for ((wrapper, backing, description) in listOf(
            Triple("LegacyPayload", "Payload", "Use Payload directly"),
            Triple("LegacyId", "VedtakId", "Use VedtakId directly")
        )) {
            val schema = schemas.path(wrapper)
            assertFalse(schema.has("\$ref"))
            assertEquals("#/components/schemas/$backing", schema.path("allOf").single().path("\$ref").asText())
            assertEquals(listOf("\$ref"), schema.path("allOf").single().fieldNames().asSequence().toList())
            assertTrue(schema.path("deprecated").asBoolean(), wrapper)
            assertEquals(description, schema.path("description").asText())
            assertFalse(schemas.path(backing).has("deprecated"), backing)
            assertFalse(schemas.path(backing).has("description"), backing)
        }
        val annotated = schemas.path("AnnotatedPayload")
        assertFalse(annotated.has("\$ref"))
        assertEquals("#/components/schemas/Payload", annotated.path("allOf").single().path("\$ref").asText())
        assertEquals(listOf("\$ref"), annotated.path("allOf").single().fieldNames().asSequence().toList())
        assertTrue(annotated.path("deprecated").asBoolean())
        assertEquals("Backing property deprecation", annotated.path("description").asText())
        val usage = properties.path("hAnnotatedPayload")
        assertFalse(usage.has("\$ref"))
        assertEquals(
            "#/components/schemas/AnnotatedPayload",
            usage.path("allOf").single().path("\$ref").asText()
        )
        assertEquals("Usage description", usage.path("description").asText())
        assertFalse(usage.has("deprecated"))
    }

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
