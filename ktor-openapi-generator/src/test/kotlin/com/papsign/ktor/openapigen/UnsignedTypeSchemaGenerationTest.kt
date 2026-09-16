package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.papsign.ktor.openapigen.annotations.Request
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Request("Forespørsel")
@Response("Svar")
data class UsignerteHeltall(
    val enUByte: UByte,
    val enUShort: UShort,
    val enUInt: UInt,
    val enULong: ULong,
)

internal class UnsignedTypeSchemaGenerationTest {

    @Test
    fun `usignerte heltall genererer integer-skjema med minimum 0`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("unsigned") {
                    post<Unit, UsignerteHeltall, UsignerteHeltall> { _, body -> respond(body) }
                }
            }
        }
        val response = client.get("http://localhost/openapi.json")
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()

        assertTrue(body.contains(""""enUByte" : {
            "maximum" : 255,
            "minimum" : 0,
            "nullable" : false,
            "type" : "integer"
          }"""))
        assertTrue(body.contains(""""enUShort" : {
            "maximum" : 65535,
            "minimum" : 0,
            "nullable" : false,
            "type" : "integer"
          }"""))
        assertTrue(body.contains(""""enUInt" : {
            "maximum" : 4294967295,
            "minimum" : 0,
            "nullable" : false,
            "type" : "integer"
          }"""))
        assertTrue(body.contains(""""enULong" : {
            "maximum" : 18446744073709551615,
            "minimum" : 0,
            "nullable" : false,
            "type" : "integer"
          }"""))
    }
}
