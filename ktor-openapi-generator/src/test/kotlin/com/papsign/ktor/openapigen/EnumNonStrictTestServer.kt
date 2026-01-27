package com.papsign.ktor.openapigen

import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.exceptions.OpenAPIBadContentException
import com.papsign.ktor.openapigen.exceptions.OpenAPIRequiredFieldException
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.testing.*
import kotlin.test.*

enum class NonStrictTestEnum {
    VALID,
    ALSO_VALID,
}

@Path("/")
data class NullableNonStrictEnumParams(@QueryParam("") val type: NonStrictTestEnum? = null)

@Path("/")
data class NonNullableNonStrictEnumParams(@QueryParam("") val type: NonStrictTestEnum)

class NonStrictEnumTestServer {

    companion object {
        // test server for nullable enums
        private fun Application.nullableEnum() {
            install(OpenAPIGen)
            install(StatusPages) {
                exception<OpenAPIBadContentException> { call, e ->
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                }
            }
            apiRouting {
                get<NullableNonStrictEnumParams, String> { params ->
                    if (params.type != null)
                        assertTrue { NonStrictTestEnum.values().contains(params.type) }
                    respond(params.type?.toString() ?: "null")
                }
            }
        }

        // test server for non-nullable enums
        private fun Application.nonNullableEnum() {
            install(OpenAPIGen)
            install(StatusPages) {
                exception<OpenAPIRequiredFieldException> { call, e ->
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                }
                exception<OpenAPIBadContentException> { call, e ->
                    call.respond(HttpStatusCode.BadRequest, e.localizedMessage)
                }
            }
            apiRouting {
                get<NonNullableNonStrictEnumParams, String> { params ->
                    assertTrue { NonStrictTestEnum.values().contains(params.type) }
                    respond(params.type.toString())
                }
            }
        }
    }

    @Test
    fun `nullable enum could be omitted and it will be null`() {
        testApplication {
            application {
                nullableEnum()
            }
            client.get("http://localhost/").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("null", bodyAsText())
            }
        }
    }

    @Test
    fun `nullable enum should be parsed correctly`() {
        testApplication {
            application {
                nullableEnum()
            }
            client.get("http://localhost/?type=VALID").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("VALID", bodyAsText())
            }
            client.get("http://localhost/?type=ALSO_VALID").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("ALSO_VALID", bodyAsText())
            }
        }
    }

    @Test
    fun `nullable enum parsing should be case-sensitive and should return 200 with null result`() {
        testApplication {
            application {
                nullableEnum()
            }
            client.get("http://localhost/?type=valid").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("null", bodyAsText())
            }
            client.get("http://localhost/?type=also_valid").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("null", bodyAsText())
            }
        }
    }

    @Test
    fun `nullable enum parsing should return 200 with null result on parse values outside of enum`() {
        testApplication {
            application {
                nullableEnum()
            }
            client.get("http://localhost/?type=what").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("null", bodyAsText())
            }
        }
    }

    @Test
    fun `non-nullable enum cannot be omitted`() {
        testApplication {
            application {
                nonNullableEnum()
            }
            client.get("http://localhost/").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("The field type is required", bodyAsText())
            }
        }
    }

    @Test
    fun `non-nullable enum should be parsed correctly`() {
        testApplication {
            application {
                nonNullableEnum()
            }
            client.get("http://localhost/?type=VALID").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("VALID", bodyAsText())
            }
            client.get("http://localhost/?type=ALSO_VALID").apply {
                assertEquals(HttpStatusCode.OK, status)
                assertEquals("ALSO_VALID", bodyAsText())
            }
        }
    }

    @Test
    fun `non-nullable enum parsing should be case-sensitive and should throw on passing wrong case`() {
        testApplication {
            application {
                nonNullableEnum()
            }
            client.get("http://localhost/?type=valid").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("The field type is required", bodyAsText())
            }
            client.get("http://localhost/?type=also_valid").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("The field type is required", bodyAsText())
            }
        }
    }

    @Test
    fun `non-nullable enum parsing should not parse values outside of enum`() {
        testApplication {
            application {
                nonNullableEnum()
            }
            client.get("http://localhost/?type=what").apply {
                assertEquals(HttpStatusCode.BadRequest, status)
                assertEquals("The field type is required", bodyAsText())
            }
        }
    }
}
