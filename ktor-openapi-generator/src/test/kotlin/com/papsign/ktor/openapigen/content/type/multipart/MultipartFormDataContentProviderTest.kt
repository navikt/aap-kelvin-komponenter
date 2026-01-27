package com.papsign.ktor.openapigen.content.type.multipart

import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import installJackson
import installOpenAPI
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.http.content.PartData
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.test.assertEquals

class MultipartFormDataContentProviderTest {

    @FormDataRequest
    data class SimpleRequest(val str: String,
                             val int: Int,
                             val flt: Float,
                             val bl: Boolean,
                             val strn: String?,
                             val intn: Int?,
                             val fltn: Float?,
                             val bln: Boolean?) {
        fun toParts(): List<PartData> {
            return this::class.declaredMemberProperties.mapNotNull {
                val prop = it as KProperty1<SimpleRequest, Any?>
                val res = prop.get(this) ?: return@mapNotNull null
                PartData.FormItem(
                    res.toString(), { }, headersOf(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Inline
                            .withParameter(ContentDisposition.Parameters.Name, it.name)
                            .toString()
                    )
                )
            }
        }
    }


    @Test
    fun testMultipartParsing() {
        val requests = mapOf(
            "/1" to SimpleRequest("Test", 300, 26.95f, true, null, null, null, null).let { Pair(it, it.toParts()) },
            "/2" to SimpleRequest("Test", 300, 26.95f, true, "Test", 300, 26.95f, true).let { Pair(it, it.toParts()) },
            "/3" to Pair(
                SimpleRequest("", 0, 0f, false, null, null, null, null), listOf(
                    PartData.FormItem(
                        "yolo", { }, headersOf(
                            HttpHeaders.ContentDisposition,
                            ContentDisposition.Inline
                                .withParameter(ContentDisposition.Parameters.Name, "yolo")
                                .toString()
                        )
                    )
                )
            )
        )
        testApplication {
            application {
                installOpenAPI()
                installJackson()
                apiRouting {
                    requests.forEach { (t, u) ->
                        route(t) {
                            post<Unit, Boolean, SimpleRequest> { _, body ->
                                assertEquals(u.first, body)
                                respond(true)
                            }
                        }
                    }
                }
            }
            requests.forEach { (t, u) ->
                println("Test: $t")
                val boundary = "***bbb***"
                client.post("http://localhost" + t) {
                    header(
                        HttpHeaders.ContentType,
                        ContentType.MultiPart.FormData.withParameter("boundary", boundary).toString()
                    )
                    setBody(
                        MultiPartFormDataContent(
                            u.second,
                            boundary,
                            ContentType.MultiPart.FormData.withParameter("boundary", boundary)
                        )
                    )
                }.apply {
                    assertEquals(
                        true,
                        bodyAsText().toBoolean()
                    )
                }
                println("Test: $t success")
            }
        }
    }
}
