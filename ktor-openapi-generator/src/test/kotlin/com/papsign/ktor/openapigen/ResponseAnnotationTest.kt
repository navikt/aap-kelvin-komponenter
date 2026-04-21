package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.responseDescription
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class ResponseAnnotationTest {

    @Response(description = "Ressursen ble funnet", statusCode = 200)
    data class ResponseMed200(val verdi: String)

    @Response(description = "Ressursen ble opprettet", statusCode = 201)
    data class ResponseMed201(val verdi: String)

    @Test
    fun `@Response description vises i generert OpenAPI JSON`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test-response") {
                    get<Unit, ResponseMed200> {
                        respond(ResponseMed200("ok"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText()).contains(""""description" : "Ressursen ble funnet"""")
        }
    }

    @Test
    fun `@Response statusCode vises i generert OpenAPI JSON`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test-response-201") {
                    get<Unit, ResponseMed201> {
                        respond(ResponseMed201("opprettet"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText())
                .contains(""""201"""")
                .contains(""""description" : "Ressursen ble opprettet"""")
        }
    }

    @Test
    fun `responseDescription vises i generert OpenAPI JSON for List av T`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test-list-response") {
                    get<Unit, List<ResponseMed200>>(responseDescription("Liste av ressurser")) {
                        respond(listOf(ResponseMed200("ok")))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText()).contains(""""description" : "Liste av ressurser"""")
        }
    }

    @Test
    fun `responseDescription overstyrer @Response description`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("test-override") {
                    get<Unit, ResponseMed200>(responseDescription("Overstyrte beskrivelse")) {
                        respond(ResponseMed200("ok"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertThat(bodyAsText())
                .contains(""""description" : "Overstyrte beskrivelse"""")
                .doesNotContain(""""description" : "Ressursen ble funnet"""")
        }
    }
}
