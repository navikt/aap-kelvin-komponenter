package com.papsign.ktor.openapigen.routing

import com.papsign.ktor.openapigen.annotations.parameters.HeaderParam
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import installJackson
import installOpenAPI
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.routing.Routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GenericsTest {

    data class TestHeaderParams(@param:HeaderParam("test param") val `Test-Header`: MutableList<Long>)

    @Test
    fun testTypedMap() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        post<TestHeaderParams, List<String>, Map<String, String>> { params, body ->
                            respond(mutableListOf(params.toString(), body.toString()))
                        }
                    }
                }
            }
            client.post("http://localhost/" + route) {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                header("Test-Header", "1,2,3")
                setBody("{\"xyz\":456}")
            }.apply {
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "[\"TestHeaderParams(Test-Header=[1, 2, 3])\",\"{xyz=456}\"]")
            }
        }
    }

    @Test
    fun testTypedList() {
        val route = "/test"
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    (this.ktorRoute as Routing).trace { println(it.buildText()) }
                    route(route) {
                        post<TestHeaderParams, List<String>, List<String>> { params, body ->
                            respond(mutableListOf(params.toString(), body.toString()))
                        }
                    }
                }
            }
            client.post("http://localhost/" + route) {
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                header("Test-Header", "1,2,3")
                setBody("[\"a\",\"b\",\"c\"]")
            }.apply {
                assertTrue { contentType()?.match(Application.Json) == true }
                assertEquals(bodyAsText(), "[\"TestHeaderParams(Test-Header=[1, 2, 3])\",\"[a, b, c]\"]")
            }
        }
    }

}
