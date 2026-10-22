package com.papsign.ktor.openapigen

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.server.application.install
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "meldingType",
    visible = true
)
sealed interface Melding

sealed interface Klage : Melding

data class KlageV0(val kravMottatt: LocalDate) : Klage

class NestedSealedDiscriminatorTest {
    private val schemaPrefix = "com.papsign.ktor.openapigen"

    private fun openApi(): JsonNode {
        var result: JsonNode? = null
        testApplication {
            application {
                val generator = install(OpenAPIGen) { }
                apiRouting {
                    route("/melding") {
                        post<Unit, Unit, Melding> { _, _ -> respond(Unit) }
                    }
                }
                result = ObjectMapper().registerModule(JavaTimeModule()).valueToTree(generator.api.serialize())
            }
            startApplication()
        }
        return result!!
    }

    @Test
    fun `nested sealed discriminator targets are compatible with openapi-typescript`() {
        val document = openApi()
        val schemas = document.at("/components/schemas")
        val discriminatorRef = "#/components/schemas/$schemaPrefix.Melding"
        val melding = schemas.path("$schemaPrefix.Melding")
        assertEquals("meldingType", melding.at("/discriminator/propertyName").asText())
        assertEquals(
            "#/components/schemas/$schemaPrefix.KlageV0",
            melding.at("/discriminator/mapping/KlageV0").asText()
        )
        for (name in listOf("Melding", "Klage")) {
            assertEquals(
                listOf("#/components/schemas/$schemaPrefix.KlageV0"),
                schemas.path("$schemaPrefix.$name").path("oneOf").map { it.path("\$ref").asText() },
                name
            )
        }

        // openapi-typescript also infers mappings from oneOf refs, even with an explicit mapping.
        val mappings = melding.path("oneOf").associate {
            val ref = it.path("\$ref").asText()
            ref to ref.substringAfterLast("/")
        }.toMutableMap()
        for ((id, target) in melding.at("/discriminator/mapping").properties()) {
            mappings[target.asText()] = id
        }
        for ((ref, id) in mappings) {
            val target = document.at(ref.removePrefix("#"))
            assertTrue(
                target.path("type").asText() == "object" || target.path("allOf").isArray,
                "Discriminator mapping has an invalid schema (neither an object schema nor an allOf array): " +
                    "$ref => $id (Discriminator: $discriminatorRef)\nGenerated OpenAPI: $document"
            )
        }
    }
}
