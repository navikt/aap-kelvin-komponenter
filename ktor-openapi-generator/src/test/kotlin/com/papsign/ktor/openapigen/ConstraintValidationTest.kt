package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.annotations.type.number.floating.clamp.FClamp
import com.papsign.ktor.openapigen.annotations.type.number.floating.max.FMax
import com.papsign.ktor.openapigen.annotations.type.number.floating.min.FMin
import com.papsign.ktor.openapigen.annotations.type.number.integer.clamp.Clamp
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.annotations.type.string.length.Length
import com.papsign.ktor.openapigen.annotations.type.string.length.MaxLength
import com.papsign.ktor.openapigen.annotations.type.string.length.MinLength
import com.papsign.ktor.openapigen.annotations.type.string.pattern.RegularExpression
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class ConstraintValidationTest {

    @Request("Forespørsel")
    @Response("Svar")
    data class MedHeltallsgrenser(
        @Min(1) val nedre: Int,
        @Max(99) val øvre: Int,
        @Clamp(5, 50) val mellom: Int,
    )

    @Request("Forespørsel")
    @Response("Svar")
    data class MedDesimalgrenser(
        @FMin(0.0) val nedre: Double,
        @FMax(1.0) val øvre: Double,
        @FClamp(0.1, 0.9) val mellom: Double,
    )

    @Request("Forespørsel")
    @Response("Svar")
    data class MedTekstgrenser(
        @MinLength(3) val kortNedre: String,
        @MaxLength(5) val kortØvre: String,
        @Length(2, 4) val mellom: String,
        @RegularExpression("^[a-z]+$") val mønster: String,
    )

    @Language("JSON") private val gyldigHeltall = """{"nedre":5,"øvre":50,"mellom":25}"""
    @Language("JSON") private val gyldigDesimal = """{"nedre":0.5,"øvre":0.5,"mellom":0.5}"""
    @Language("JSON") private val gyldigTekst = """{"kortNedre":"abc","kortØvre":"ab","mellom":"ab","mønster":"abc"}"""

    private suspend fun postJson(client: HttpClient, path: String, @Language("JSON") body: String) =
        client.post(path) {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            setBody(body)
        }

    @Test
    fun `@Min avviser verdi under grensen`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedHeltallsgrenser, MedHeltallsgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(client, "/test", """{"nedre":0,"øvre":50,"mellom":25}""").status
        val gyldig = postJson(client, "/test", gyldigHeltall).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@Max avviser verdi over grensen`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedHeltallsgrenser, MedHeltallsgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(client, "/test", """{"nedre":1,"øvre":100,"mellom":25}""").status
        val gyldig = postJson(client, "/test", gyldigHeltall).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@Clamp avviser verdier utenfor intervallet`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedHeltallsgrenser, MedHeltallsgrenser> { _, body -> respond(body) }
                }
            }
        }
        val forLav = postJson(client, "/test", """{"nedre":1,"øvre":50,"mellom":4}""").status
        val forHøy = postJson(client, "/test", """{"nedre":1,"øvre":50,"mellom":51}""").status
        val gyldig = postJson(client, "/test", gyldigHeltall).status
        assertThat(forLav).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(forHøy).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@FMin avviser verdi under grensen`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedDesimalgrenser, MedDesimalgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(client, "/test", """{"nedre":-0.1,"øvre":0.5,"mellom":0.5}""").status
        val gyldig = postJson(client, "/test", gyldigDesimal).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@FMax avviser verdi over grensen`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedDesimalgrenser, MedDesimalgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(client, "/test", """{"nedre":0.5,"øvre":1.1,"mellom":0.5}""").status
        val gyldig = postJson(client, "/test", gyldigDesimal).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@FClamp avviser verdier utenfor intervallet`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedDesimalgrenser, MedDesimalgrenser> { _, body -> respond(body) }
                }
            }
        }
        val forLav = postJson(client, "/test", """{"nedre":0.5,"øvre":0.5,"mellom":0.05}""").status
        val forHøy = postJson(client, "/test", """{"nedre":0.5,"øvre":0.5,"mellom":0.95}""").status
        val gyldig = postJson(client, "/test", gyldigDesimal).status
        assertThat(forLav).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(forHøy).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@MinLength avviser for kort streng`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedTekstgrenser, MedTekstgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(
            client, "/test",
            """{"kortNedre":"ab","kortØvre":"ab","mellom":"ab","mønster":"abc"}"""
        ).status
        val gyldig = postJson(client, "/test", gyldigTekst).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@MaxLength avviser for lang streng`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedTekstgrenser, MedTekstgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(
            client, "/test",
            """{"kortNedre":"abc","kortØvre":"toolong","mellom":"ab","mønster":"abc"}"""
        ).status
        val gyldig = postJson(client, "/test", gyldigTekst).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@Length avviser strenger utenfor lengdegrensene`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedTekstgrenser, MedTekstgrenser> { _, body -> respond(body) }
                }
            }
        }
        val forKort = postJson(
            client, "/test",
            """{"kortNedre":"abc","kortØvre":"ab","mellom":"a","mønster":"abc"}"""
        ).status
        val forLang = postJson(
            client, "/test",
            """{"kortNedre":"abc","kortØvre":"ab","mellom":"abcde","mønster":"abc"}"""
        ).status
        val gyldig = postJson(client, "/test", gyldigTekst).status
        assertThat(forKort).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(forLang).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }

    @Test
    fun `@RegularExpression avviser streng som ikke matcher mønsteret`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test") {
                    post<Unit, MedTekstgrenser, MedTekstgrenser> { _, body -> respond(body) }
                }
            }
        }
        val ugyldig = postJson(
            client, "/test",
            """{"kortNedre":"abc","kortØvre":"ab","mellom":"ab","mønster":"ABC"}"""
        ).status
        val gyldig = postJson(client, "/test", gyldigTekst).status
        assertThat(ugyldig).isEqualTo(HttpStatusCode.BadRequest)
        assertThat(gyldig).isEqualTo(HttpStatusCode.OK)
    }
}
