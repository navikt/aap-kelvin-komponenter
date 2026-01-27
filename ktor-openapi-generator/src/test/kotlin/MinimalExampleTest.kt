import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinimalExampleTest {
    @Test
    fun `test that minimal example works`(): Unit = testApplication {
        application(Application::minimalExample)

        val client = createClient { install(ContentNegotiation) { jackson() } }

        val openapi = client.get("/openapi.json")
        val body = openapi.body<String>()
        assertEquals(HttpStatusCode.OK, openapi.status)
        assertTrue(body.isNotEmpty())

        val response = client.post("/example/world") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody(SomeRequest(foo = "hello from body"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            SomeResponse(bar = "Hello world! From body: hello from body."),
            response.body()
        )
    }

    @Test
    fun `test forbidden route and respondWithStatus`(): Unit = testApplication {
        application(Application::minimalExample)

        val client = createClient { install(ContentNegotiation) { jackson() } }

        val response = client.get("/forbidden/112") {
            header(HttpHeaders.Accept, ContentType.Application.Pdf.toString())
            header(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
        }
        assertEquals(Unit, response.body<Unit>())
    }
}
