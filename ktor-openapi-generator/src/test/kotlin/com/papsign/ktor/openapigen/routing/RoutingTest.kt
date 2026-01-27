package com.papsign.ktor.openapigen.routing

import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import installJackson
import installOpenAPI
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.Routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingTest {

    data class TestHeaderParams(@param:HeaderParam("test param") val `Test-Header`: Long)
    data class TestHeaderParams2(@param:HeaderParam("test param") val `test-header`: Long)
    data class TestBodyParams(val xyz: Long)
    data class TestResponse(val msg: String)

    @Test
    fun testPostWithHeaderAndBodyParams() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        post<TestHeaderParams, TestResponse, TestBodyParams> { params, body ->
                            respond(TestResponse("$params -> $body"))
                        }
                    }
                }
            }
            client.post("http://localhost/" + route) {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                header("test-header", "123")
                setBody("{\"xyz\":456}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "{\"msg\":\"${TestHeaderParams(123)} -> ${TestBodyParams(456)}\"}")
            }
        }
    }

    @Test
    fun testGetWithHeaderParams() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        get<TestHeaderParams2, TestResponse> { params ->
                            respond(TestResponse("$params"))
                        }
                    }
                }
            }
            client.get("http://localhost/" + route) {
                header(HttpHeaders.Accept, "application/json")
                header("Test-Header", "123")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "{\"msg\":\"${TestHeaderParams2(123)}\"}")
            }
        }
    }

    @Test
    fun testPostWithUnitTypes() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        post<Unit, TestResponse, Unit> { params, body ->
                            respond(TestResponse("Test Response"))
                        }
                    }
                }
            }
            client.post("http://localhost/" + route) {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                header("test-header", "123")
                setBody("{\"xyz\":456}")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "{\"msg\":\"Test Response\"}")
            }
        }
    }

    @Test
    fun testGetWithUnitTypes() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        get<Unit, TestResponse> { params ->
                            respond(TestResponse("Test Response"))
                        }
                    }
                }
            }
            client.get("http://localhost/" + route) {
                header(HttpHeaders.Accept, "application/json")
            }.apply {
                assertEquals(HttpStatusCode.OK, status)
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "{\"msg\":\"Test Response\"}")
            }
        }
    }
}
