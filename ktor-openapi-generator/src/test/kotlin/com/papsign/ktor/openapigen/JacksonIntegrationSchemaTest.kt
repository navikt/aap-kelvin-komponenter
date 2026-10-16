package com.papsign.ktor.openapigen

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val PKG = "com.papsign.ktor.openapigen"

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
sealed interface Blandet

@JsonTypeName("navngitt")
data class Navngitt(val type: String = "navngitt", val x: Int) : Blandet

data class Unavngitt(val type: String = "Unavngitt", val y: Int) : Blandet

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
sealed interface KlasseId

data class KlasseIdA(val a: Int) : KlasseIdA2
sealed interface KlasseIdA2 : KlasseId

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_OBJECT)
sealed interface Innpakket

data class InnpakketA(val a: Int) : Innpakket

data class GetterValue(private val intern: String) {
    @get:JsonValue
    val ekstern: String get() = intern
}

enum class MedJsonValue(@get:JsonValue val kode: String) { EN("1"), TO("2") }

enum class OmdøptKonstant {
    @JsonProperty("omdøpt")
    GAMMEL,
    BEHOLDT
}

data class Boks<T>(val verdi: T)

data class Brukere(
    val blandet: Blandet?,
    val klasse: KlasseId?,
    val innpakket: Innpakket?,
    val getter: GetterValue,
    val enumValue: MedJsonValue,
    val omdøpt: OmdøptKonstant,
    val heltall: Boks<Int>,
    val tekst: Boks<String>,
)

class JacksonIntegrationSchemaTest {
    private fun openApi(): JsonNode {
        var result: JsonNode? = null
        testApplication {
            application { setup() }
            result = ObjectMapper().readTree(client.get("/openapi.json").bodyAsText())
        }
        return result!!
    }

    private fun Application.setup() {
        install(OpenAPIGen) { }
        install(ContentNegotiation) { jackson() }
        apiRouting {
            route("/test") {
                post<Unit, Unit, Brukere> { _, _ -> respond(Unit) }
            }
        }
    }

    private val schemas get() = openApi().at("/components/schemas")

    @Test
    fun `blandet hierarki bruker Jacksons type-id for alle subtyper`() {
        val schemas = schemas
        val union = schemas.at("/$PKG.Blandet")
        assertEquals("type", union.at("/discriminator/propertyName").asText())
        assertEquals("#/components/schemas/$PKG.Navngitt", union.at("/discriminator/mapping/navngitt").asText())
        assertEquals("#/components/schemas/$PKG.Unavngitt", union.at("/discriminator/mapping/Unavngitt").asText())
        assertEquals(listOf("navngitt"), schemas.at("/$PKG.Navngitt/properties/type/enum").map { it.asText() })
        assertEquals(listOf("Unavngitt"), schemas.at("/$PKG.Unavngitt/properties/type/enum").map { it.asText() })
    }

    @Test
    fun `type-id som ikke er en egenskap gir ingen discriminator`() {
        val schemas = schemas
        assertTrue(schemas.at("/$PKG.KlasseId").has("oneOf"))
        assertTrue(schemas.at("/$PKG.KlasseId/discriminator").isMissingNode)
        assertTrue(schemas.at("/$PKG.Innpakket/discriminator").isMissingNode)
    }

    @Test
    fun `JsonValue på getter gir primitivt skjema`() {
        val getter = schemas.at("/$PKG.GetterValue")
        assertEquals("string", getter.at("/type").asText())
        assertTrue(getter.at("/properties").isMissingNode)
    }

    @Test
    fun `enum-verdier følger Jacksons serialisering`() {
        val properties = schemas.at("/$PKG.Brukere/properties")
        assertEquals(listOf("1", "2"), properties.at("/enumValue/enum").map { it.asText() })
        assertEquals(listOf("BEHOLDT", "omdøpt"), properties.at("/omdøpt/enum").map { it.asText() })
    }

    @Test
    fun `generiske typer med ulike type-argumenter kolliderer ikke`() {
        val names = schemas.fieldNames().asSequence().toList()
        assertEquals(2, names.count { it.contains("Boks") }, names.toString())
        assertNull(names.singleOrNull { it == "$PKG.Boks" })
    }
}
