package com.papsign.ktor.openapigen

import TestServer.setupBaseTestServer
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

enum class UnsortedEnum {
    ZEBRA,
    APPLE,
    MANGO,
    BANANA,
}

class EnumSortingTest {

    @Test
    fun `enum values should be sorted alphabetically in schema`() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                route("unsorted-enum") {
                    get<Unit, UnsortedEnum> {
                        respond(UnsortedEnum.APPLE)
                    }
                }
            }
        }
        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            val body = bodyAsText()
            val enumRegex = """"enum"\s*:\s*\[\s*"APPLE"\s*,\s*"BANANA"\s*,\s*"MANGO"\s*,\s*"ZEBRA"\s*]""".toRegex()
            assertTrue(enumRegex.containsMatchIn(body), "Enum values should be sorted alphabetically, got: $body")
        }
    }
}
