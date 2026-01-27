import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.BehovType
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.content.type.binary.BinaryResponse
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import java.io.InputStream

/**
 * Minimal example of OpenAPI plugin for Ktor.
 */
fun Application.minimalExample() {
    // install OpenAPI plugin
    install(OpenAPIGen) {
        // this serve OpenAPI definition on /openapi.json
        serveOpenApiJson = true
        // this servers Swagger UI on /swagger-ui/index.html
        serveSwaggerUi = true
        info {
            title = "Minimal Example API"
        }
    }
    // install JSON support
    install(ContentNegotiation) {
        jackson()
    }
    // and now example routing
    apiRouting {
        route("/example/{name}") {
            // SomeParams are parameters (query or path), SomeResponse is what the backend returns and SomeRequest
            // is what was passed in the body of the request
            post<SomeParams, SomeResponse, SomeRequest> { params, someRequest ->
                respond(SomeResponse(bar = "Hello ${params.name}! From body: ${someRequest.foo}."))
            }
            get<Unit, SomeSimpleEnum> { _ ->
                respond(SomeSimpleEnum.C)
            }
        }
        route("/not-an-example/") {
            get<Unit, SomeComplexEnum> {
                respond(SomeComplexEnum.C)
            }
        }
        route("/not-an-example-anymore/") {
            get<Unit, BehovType> {
                respond(BehovType.MANUELT_PÅKREVD)
            }
            post<Unit, BehovType, BehovType> { _, dto ->
                respond(dto)
            }
        }

        route("/forbidden/{name}") {
            get<SomeParams, DokumentResponsDTO> { params ->
                params.name
                respondWithStatus(HttpStatusCode.Forbidden)
            }
        }
    }
}

@BinaryResponse(contentTypes = ["application/pdf"])
data class DokumentResponsDTO(val stream: InputStream)

data class SomeParams(@param:PathParam("who to say hello") val name: String)
data class SomeRequest(val foo: String)
data class SomeResponse(val bar: String)

enum class SomeSimpleEnum {
    A, B, C
}

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class SomeComplexEnum(
    @param:JsonProperty("proppen") val variable: String,
) {
    A("a"), B("b"), C("c")
}
