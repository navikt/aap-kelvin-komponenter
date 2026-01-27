package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.fasterxml.jackson.annotation.JsonValue
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class JsonValueAnnotationTest {

    data class IntWrapper(@JsonValue val x: Int)

    data class StringWrapper(@JsonValue val value: String)

    data class BooleanWrapper(@JsonValue val flag: Boolean)

    data class Request(val data: String)

    @Test
    fun `test JsonValue annotation with Int creates primitive schema not object schema`() =
        testApplication {
            application {
                setupBaseTestServer()
                apiRouting {
                    route("int-wrapper") {
                        post<Unit, IntWrapper, Request> { _, _ ->
                            respond(IntWrapper(42))
                        }
                    }
                }
            }
            client.get("http://localhost/openapi.json").apply {
                assertEquals(HttpStatusCode.OK, status)
                val body = bodyAsText()

                // The response schema should reference IntWrapper
                assertTrue(body.contains("\"IntWrapper\""))

                // IntWrapper schema should be a primitive integer type, not an object with property 'x'
                // When @JsonValue is properly supported, the schema should be:
                // "IntWrapper" : { "type" : "integer", "format" : "int32", "nullable" : false }
                // NOT: "IntWrapper" : { "type" : "object", "properties" : { "x" : ... } }

                val intWrapperSchemaPattern = Regex(
                    """"IntWrapper"\s*:\s*\{[^}]*"type"\s*:\s*"integer"[^}]*\}"""
                )
                assertTrue(
                    intWrapperSchemaPattern.containsMatchIn(body),
                    "IntWrapper should have type 'integer', not 'object'"
                )

                // It should NOT have an 'x' property in the IntWrapper schema
                assertFalse(
                    body.contains(""""IntWrapper" : {""") && body.contains(""""properties"""") && body.contains(
                        """"x""""
                    ),
                    "IntWrapper schema should not have an 'x' property when @JsonValue is used"
                )
            }
        }

    @Test
    fun `test JsonValue annotation with String creates string schema not object schema`() =
        testApplication {
            application {
                setupBaseTestServer()
                apiRouting {
                    route("string-wrapper") {
                        post<Unit, StringWrapper, Request> { _, _ ->
                            respond(StringWrapper("test"))
                        }
                    }
                }
            }
            client.get("http://localhost/openapi.json").apply {
                assertEquals(HttpStatusCode.OK, status)
                val body = bodyAsText()

                // The response schema should reference StringWrapper
                assertTrue(body.contains("\"StringWrapper\""))

                // StringWrapper schema should be a primitive string type, not an object with property 'value'
                val stringWrapperSchemaPattern = Regex(
                    """"StringWrapper"\s*:\s*\{[^}]*"type"\s*:\s*"string"[^}]*\}"""
                )
                assertTrue(
                    stringWrapperSchemaPattern.containsMatchIn(body),
                    "StringWrapper should have type 'string', not 'object'"
                )

                // It should NOT have a 'value' property in the StringWrapper schema
                assertFalse(
                    body.contains(""""StringWrapper" : {""") && body.contains(""""properties"""") && body.contains(
                        """"value""""
                    ),
                    "StringWrapper schema should not have a 'value' property when @JsonValue is used"
                )
            }
        }

    @Test
    fun `test JsonValue annotation with Boolean creates boolean schema not object schema`() =
        testApplication {
            application {
                setupBaseTestServer()
                apiRouting {
                    route("boolean-wrapper") {
                        post<Unit, BooleanWrapper, Request> { _, _ ->
                            respond(BooleanWrapper(true))
                        }
                    }
                }
            }
            client.get("http://localhost/openapi.json").apply {
                assertEquals(HttpStatusCode.OK, status)
                val body = bodyAsText()

                // The response schema should reference BooleanWrapper
                assertTrue(body.contains("\"BooleanWrapper\""))

                // BooleanWrapper schema should be a primitive boolean type, not an object with property 'flag'
                val booleanWrapperSchemaPattern = Regex(
                    """"BooleanWrapper"\s*:\s*\{[^}]*"type"\s*:\s*"boolean"[^}]*\}"""
                )
                assertTrue(
                    booleanWrapperSchemaPattern.containsMatchIn(body),
                    "BooleanWrapper should have type 'boolean', not 'object'"
                )

                // It should NOT have a 'flag' property in the BooleanWrapper schema
                assertFalse(
                    body.contains(""""BooleanWrapper" : {""") && body.contains(""""properties"""") && body.contains(
                        """"flag""""
                    ),
                    "BooleanWrapper schema should not have a 'flag' property when @JsonValue is used"
                )
            }
        }
}
