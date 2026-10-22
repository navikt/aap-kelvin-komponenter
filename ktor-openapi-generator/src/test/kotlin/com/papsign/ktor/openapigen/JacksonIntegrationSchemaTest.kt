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
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import no.nav.aap.komponenter.json.DefaultJsonMapper

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
enum class NumericJsonValue(@get:JsonValue val code: Int) { ONE(1) }

enum class OmdøptKonstant {
    @JsonProperty("omdøpt")
    GAMMEL,
    BEHOLDT
}

data class Boks<T>(val verdi: T)

data class ComputedProperties(val value: String) {
    val computed: String get() = value
}

class FieldProperties(@JvmField val value: String)

interface AbstractProperties {
    val value: String
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tag")
sealed interface InjectedTag

@JsonTypeName("first")
data class FirstTag(val value: String) : InjectedTag

@JsonTypeName("second")
data class SecondTag(val value: String) : InjectedTag

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tag")
sealed interface ExistingTag

@JsonTypeName("existing")
data class DeclaredTag(val tag: String = "existing") : ExistingTag

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "tag")
sealed interface MissingExistingTag

data class UndeclaredTag(val value: String) : MissingExistingTag

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tag")
sealed interface EnumLeafUnion

enum class EnumLeaf : EnumLeafUnion { VALUE }
data class ObjectLeaf(val value: String) : EnumLeafUnion

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tag")
@JsonTypeName("base")
sealed interface ContextualTag

data class ContextualChild(val value: String) : ContextualTag

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tag")
abstract class OpenBase(val value: String)

@JsonTypeName("child")
class OpenChild(value: String) : OpenBase(value)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "tag")
sealed interface OpenBranchUnion

open class OpenBranch(val value: String) : OpenBranchUnion

data class PlainEnumContext(@get:JsonTypeInfo(use = JsonTypeInfo.Id.NONE) val value: EnumLeaf)

data class Brukere(
    val contextual: ContextualTag,
    val contextualChild: ContextualChild,
    val openBase: OpenBase,
    val openBranch: OpenBranchUnion,
    val plainEnum: PlainEnumContext,
    val computed: ComputedProperties,
    val field: FieldProperties,
    val abstract: AbstractProperties,
    val injected: InjectedTag,
    val existing: ExistingTag,
    val missingExisting: MissingExistingTag,
    val enumLeaf: EnumLeafUnion,
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
    fun `shared child schema accepts parent and standalone Jackson type ids`() {
        val mapper = DefaultJsonMapper.objectMapper()
        val child = ContextualChild("value")
        val parentJson = mapper.readTree(mapper.writerFor(ContextualTag::class.java).writeValueAsString(child))
        val childJson = mapper.valueToTree<JsonNode>(child)
        assertNotEquals(parentJson.path("tag").asText(), childJson.path("tag").asText())
        val schemas = schemas
        val ids = schemas.at("/$PKG.ContextualChild/properties/tag/enum").map { it.asText() }
        assertTrue(parentJson.path("tag").asText() in ids, ids.toString())
        assertTrue(childJson.path("tag").asText() in ids, ids.toString())
        assertEquals(
            "#/components/schemas/$PKG.ContextualChild",
            schemas.at("/$PKG.ContextualTag/discriminator/mapping").path(parentJson.path("tag").asText()).asText()
        )
    }

    @Test
    fun `open types are not constrained to a singleton type id`() {
        val schemas = schemas
        val mapper = DefaultJsonMapper.objectMapper()
        val json = mapper.readTree(mapper.writerFor(OpenBase::class.java).writeValueAsString(OpenChild("value")))
        assertEquals("child", json.path("tag").asText())
        assertTrue(schemas.at("/$PKG.OpenBase/properties/tag").isMissingNode)
        assertTrue(schemas.at("/$PKG.OpenBranch/properties/tag").isMissingNode)
        assertTrue(schemas.at("/$PKG.OpenBranchUnion/discriminator").isMissingNode)
    }

    @Test
    fun `polymorphic enums retain value names rather than empty strings`() {
        val schemas = schemas
        val enumSchema = schemas.at("/$PKG.EnumLeafUnion/oneOf").single { it.has("enum") }
        assertEquals(listOf("VALUE"), enumSchema.path("enum").map { it.asText() })
        assertEquals(
            listOf("VALUE"),
            schemas.at("/$PKG.PlainEnumContext/properties/value/enum").map { it.asText() }
        )
        val mapper = DefaultJsonMapper.objectMapper()
        assertEquals("VALUE", mapper.valueToTree<JsonNode>(PlainEnumContext(EnumLeaf.VALUE)).path("value").asText())
    }

    @Test
    fun `non-string enum representations fail explicitly`() {
        val error = assertFailsWith<IllegalArgumentException> {
            testApplication {
                application {
                    install(OpenAPIGen) { }
                    apiRouting {
                        route("/numeric-enum") {
                            post<Unit, Unit, NumericJsonValue> { _, _ -> respond(Unit) }
                        }
                    }
                }
                startApplication()
            }
        }
        assertTrue(error.message.orEmpty().contains("Only string enum values are supported"))
    }

    @Test
    fun `ordinary properties without JsonValue preserve object schemas`() {
        val schemas = schemas
        for (name in listOf("ComputedProperties", "FieldProperties", "AbstractProperties")) {
            val schema = schemas.path("$PKG.$name")
            assertEquals("object", schema.path("type").asText(), name)
            assertEquals("string", schema.at("/properties/value/type").asText(), name)
        }
        assertEquals("string", schemas.at("/$PKG.ComputedProperties/properties/computed/type").asText())
    }

    @Test
    fun `injected tags are required and mapping targets exist`() {
        val schemas = schemas
        val mapping = schemas.at("/$PKG.InjectedTag/discriminator/mapping")
        assertEquals(2, mapping.size())
        for ((id, target) in mapping.properties()) {
            val schema = schemas.path(target.asText().removePrefix("#/components/schemas/"))
            assertEquals(listOf(id), schema.at("/properties/tag/enum").map { it.asText() })
            assertTrue("tag" in schema.path("required").map { it.asText() })
        }
    }

    @Test
    fun `existing property is narrowed but missing property is not synthesized`() {
        val schemas = schemas
        assertEquals(listOf("existing"), schemas.at("/$PKG.DeclaredTag/properties/tag/enum").map { it.asText() })
        assertEquals("tag", schemas.at("/$PKG.ExistingTag/discriminator/propertyName").asText())
        assertTrue(schemas.at("/$PKG.UndeclaredTag/properties/tag").isMissingNode)
        assertTrue(schemas.at("/$PKG.MissingExistingTag/discriminator").isMissingNode)
    }

    @Test
    fun `enum leaves do not produce dangling discriminator mappings`() {
        val union = schemas.path("$PKG.EnumLeafUnion")
        assertEquals(2, union.path("oneOf").size())
        assertTrue(union.path("discriminator").isMissingNode)
        assertTrue(union.path("oneOf").any { it.has("enum") })
    }

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
        assertEquals(
            listOf("#/components/schemas/$PKG.KlasseIdA2"),
            schemas.at("/$PKG.KlasseId/oneOf").map { it.path("\$ref").asText() }
        )
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
