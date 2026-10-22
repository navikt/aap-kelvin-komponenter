package com.papsign.ktor.openapigen

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

enum class Kode { A, B, C }

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "kode", visible = true)
sealed interface Løsning

@JsonTypeName("A")
data class LøsningA(val kode: Kode = Kode.A, val a: String) : Løsning

@JsonTypeName("B")
data class LøsningB(val kode: Kode = Kode.B, val b: Int) : Løsning

sealed interface Generisk<T> : Løsning

@JsonTypeName("C")
data class LøsningC(val kode: Kode = Kode.C, val c: List<String>) : Generisk<String>

class SealedDiscriminatorTest {
    private val schemaPrefix = "com.papsign.ktor.openapigen"

    private fun openApi(): JsonNode {
        var result: JsonNode? = null
        testApplication {
            application {
                install(OpenAPIGen) { }
                install(ContentNegotiation) { jackson() }
                apiRouting {
                    route("/test") {
                        post<Unit, Unit, Løsning> { _, _ -> respond(Unit) }
                    }
                }
            }
            result = ObjectMapper().readTree(client.get("/openapi.json").bodyAsText())
        }
        return result!!
    }

    @Test
    fun `hver subklasse har bare sin egen verdi i discriminator-feltet`() {
        val schemas = openApi().at("/components/schemas")
        for (n in listOf("A", "B", "C")) {
            assertEquals(
                listOf(n),
                schemas.at("/$schemaPrefix.Løsning$n/properties/kode/enum").map { it.asText() })
        }
    }

    @Test
    fun `sealed union har discriminator med mapping, også gjennom nested sealed`() {
        val union = openApi().at("/components/schemas/$schemaPrefix.Løsning")
        assertEquals("kode", union.at("/discriminator/propertyName").asText())
        assertEquals(
            setOf("A", "B", "C").map { "#/components/schemas/$schemaPrefix.Løsning$it" }.toSet(),
            union.path("oneOf").map { it.path("\$ref").asText() }.toSet()
        )
        for (n in listOf("A", "B", "C")) {
            assertEquals(
                "#/components/schemas/$schemaPrefix.Løsning$n",
                union.at("/discriminator/mapping/$n").asText()
            )
        }
    }

    @Test
    fun `schemanavn for generiske typer har ikke type-argument-suffiks`() {
        val names = openApi().at("/components/schemas").fieldNames().asSequence().toList()
        assertTrue(names.none { it.contains("<") }, names.toString())
    }
}
