package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.fasterxml.jackson.databind.ObjectMapper
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.annotations.type.number.floating.clamp.FClamp
import com.papsign.ktor.openapigen.annotations.type.number.floating.max.FMax
import com.papsign.ktor.openapigen.annotations.type.number.floating.min.FMin
import com.papsign.ktor.openapigen.annotations.type.number.integer.clamp.Clamp
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.annotations.type.string.example.StringExample
import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import com.papsign.ktor.openapigen.annotations.type.string.length.MaxLength
import com.papsign.ktor.openapigen.annotations.type.string.length.MinLength
import com.papsign.ktor.openapigen.annotations.type.string.pattern.RegularExpression
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class FieldAnnotationTest {

    @Response("Svar")
    data class MedDescription(
        @property:Description("Unik identifikator") val id: String,
    )

    @Response("Svar")
    data class MedDeprecated(
        val nyttFelt: String,
        @property:Deprecated("Bruk nyttFelt") val gammeltFelt: String? = null,
    )

    @Response("Svar")
    data class MedStringExample(
        @StringExample("abc-123") val id: String,
    )

    @Response("Svar")
    data class MedHeltallsgrenser(
        @Min(1) val nedre: Int,
        @Max(99) val øvre: Int,
        @Clamp(5, 50) val mellom: Int,
    )

    @Response("Svar")
    data class MedDesimalgrenser(
        @FMin(0.0) val nedre: Double,
        @FMax(1.0) val øvre: Double,
        @FClamp(0.1, 0.9) val mellom: Double,
    )

    @Response("Svar")
    data class MedTekstgrenser(
        @MinLength(2) val kortNedre: String,
        @MaxLength(20) val kortØvre: String,
        @Length(3, 15) val mellom: String,
        @RegularExpression("^[a-z]+$") val mønster: String,
    )

    @Test
    fun `@Deprecated vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") { get<Unit, MedDeprecated> { respond(MedDeprecated("ny")) } }
            }
        }
        client.get("/openapi.json").apply {
            val json = bodyAsText()
            assertThat(json).contains(""""deprecated" : true""")
            assertThat(json).contains(""""description" : "Bruk nyttFelt"""")
        }
    }

    @Test
    fun `@Description vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") { get<Unit, MedDescription> { respond(MedDescription("1")) } }
            }
        }
        client.get("/openapi.json").apply {
            assertThat(bodyAsText()).contains(""""description" : "Unik identifikator"""")
        }
    }

    @Test
    fun `@StringExample vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") { get<Unit, MedStringExample> { respond(MedStringExample("abc-123")) } }
            }
        }
        client.get("/openapi.json").apply {
            assertThat(bodyAsText()).contains(""""example" : "abc-123"""")
        }
    }

    @Test
    fun `@Min og @Max og @Clamp vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") { get<Unit, MedHeltallsgrenser> { respond(MedHeltallsgrenser(1, 99, 25)) } }
            }
        }
        client.get("/openapi.json").apply {
            val json = bodyAsText()
            assertThat(json)
                .contains(""""minimum" : 1""")
                .contains(""""maximum" : 99""")
                .contains(""""minimum" : 5""")
                .contains(""""maximum" : 50""")
        }
    }

    @Test
    fun `@FMin og @FMax og @FClamp vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") { get<Unit, MedDesimalgrenser> { respond(MedDesimalgrenser(0.0, 1.0, 0.5)) } }
            }
        }
        client.get("/openapi.json").apply {
            val json = bodyAsText()
            assertThat(json)
                .contains(""""minimum" : 0.0""")
                .contains(""""maximum" : 1.0""")
                .contains(""""minimum" : 0.1""")
                .contains(""""maximum" : 0.9""")
        }
    }

    @Test
    fun `@MinLength og @MaxLength og @Length og @RegularExpression vises i schema for responsfelt`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    get<Unit, MedTekstgrenser> {
                        respond(MedTekstgrenser("ab", "ok", "abc", "abc"))
                    }
                }
            }
        }
        client.get("/openapi.json").apply {
            val json = bodyAsText()
            assertThat(json)
                .contains(""""minLength" : 2""")
                .contains(""""maxLength" : 20""")
                .contains(""""minLength" : 3""")
                .contains(""""maxLength" : 15""")
                .contains(""""pattern" : "^[a-z]+$"""")
        }
    }

    // Delt kompleks type som refereres fra to ulike klasser
    data class DeltType(val verdi: String)

    @Response("Svar")
    data class UtenDeprecatedRef(
        val element: DeltType? = null,
    )

    @Response("Svar")
    data class MedDeprecatedRef(
        @Deprecated("Ikke i bruk av konsument.")
        val element: DeltType? = null,
    )

    @Test
    fun `@Deprecated på property forurenser ikke globalt type-skjema`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("a") { get<Unit, UtenDeprecatedRef> { respond(UtenDeprecatedRef()) } }
                route("b") { get<Unit, MedDeprecatedRef> { respond(MedDeprecatedRef()) } }
            }
        }
        client.get("/openapi.json").apply {
            val json = bodyAsText()
            val tree = ObjectMapper().readTree(json)
            val deltTypeSchema = tree.path("components").path("schemas").path("DeltType")

            assertThat(deltTypeSchema.isMissingNode)
                .describedAs("DeltType skal finnes i components/schemas")
                .isFalse()
            assertThat(deltTypeSchema.path("deprecated").isMissingNode)
                .describedAs("DeltType skal ikke være deprecated globalt – bare propertyen i MedDeprecatedRef er deprecated")
                .isTrue()
        }
    }
}
