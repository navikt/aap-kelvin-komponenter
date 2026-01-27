package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.papsign.ktor.openapigen.content.type.multipart.FormDataRequest
import com.papsign.ktor.openapigen.content.type.multipart.FormDataRequestType
import com.papsign.ktor.openapigen.content.type.multipart.NamedFileInputStream
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class FormDocumentationGenerationTest {

    @Test
    fun formDataTestRequest() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("form-data") {
                    post<Unit, TestServer.StringResponse, FormData> { _, _ ->
                        respond(TestServer.StringResponse("result"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(
                bodyAsText().contains(
                    """  "paths" : {
    "/form-data" : {
      "post" : {
        "requestBody" : {
          "content" : {
            "application/x-www-form-urlencoded" : {
              "schema" : {
                "${"$"}ref" : "#/components/schemas/FormData"
              }
            }
          }
        },"""
                )
            )
        }
    }

    @Test
    fun multipartFormDataTestRequest() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("multipart-data") {
                    post<Unit, TestServer.StringResponse, MultiPartForm> { _, _ ->
                        respond(TestServer.StringResponse("result"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(
                bodyAsText().contains(
                    """  "paths" : {
    "/multipart-data" : {
      "post" : {
        "requestBody" : {
          "content" : {
            "multipart/form-data" : {
              "schema" : {
                "${"$"}ref" : "#/components/schemas/MultiPartForm"
              }
            }
          }
        },"""
                )
            )
        }
    }

    @Test
    fun defaultFormDataTestRequest() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("default-form-data") {
                    post<Unit, TestServer.StringResponse, DefaultFormData> { _, _ ->
                        respond(TestServer.StringResponse("result"))
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertTrue(
                bodyAsText().contains(
                    """  "paths" : {
    "/default-form-data" : {
      "post" : {
        "requestBody" : {
          "content" : {
            "multipart/form-data" : {
              "schema" : {
                "${"$"}ref" : "#/components/schemas/DefaultFormData"
              }
            }
          }
        },"""
                )
            )
        }
    }
}

@FormDataRequest(type = FormDataRequestType.MULTIPART)
data class MultiPartForm(val userId: String, val file: NamedFileInputStream)

@FormDataRequest(type = FormDataRequestType.URL_ENCODED)
data class FormData(val login: String, val password: String)

@FormDataRequest
data class DefaultFormData(val login: String, val password: String)
