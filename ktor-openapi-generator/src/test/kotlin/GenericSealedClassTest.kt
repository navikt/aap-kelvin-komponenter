import TestServer.setupBaseTestServer
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import com.papsign.ktor.openapigen.annotations.type.string.example.DiscriminatorAnnotation
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

fun NormalOpenAPIRoute.GenericSealedRoute() {
    route("sealed") {
        post<Unit, Either<String, Boolean>, Either<Int, Char>>(
            info("Generic Sealed class Endpoint", "This is a Generic Sealed class Endpoint"),
            exampleRequest = Left(0),
            exampleResponse = Right(true)
        ) { _, _ ->
            respond(Right(false))
        }
    }
}


@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@DiscriminatorAnnotation()
sealed class Either<A, B>

@JsonTypeName("left")
@DiscriminatorAnnotation()
class Left<X, Y>(val left: X) : Either<X, Y>()

@JsonTypeName("right")
@DiscriminatorAnnotation()
class Right<A, B>(val right: B) : Either<A, B>()


internal class GenericSealedGenerationTests {
    @Test
    fun willDiscriminatorsBePresent() = testApplication {
        application {
            setupBaseTestServer()
            apiRouting {
                GenericSealedRoute()
            }
        }

        client.get("http://localhost/openapi.json").apply {
            assertEquals(HttpStatusCode.OK, status)
            val bodyAsText = bodyAsText()
            assertTrue(bodyAsText.contains("#/components/schemas/Left_String_Boolean_"))
            assertTrue(bodyAsText.contains("#/components/schemas/Left_Int_Char_"))
        }
    }
}
