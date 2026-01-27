import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.annotations.Path
import com.papsign.ktor.openapigen.annotations.Response
import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.properties.description.Description
import com.papsign.ktor.openapigen.model.Described
import com.papsign.ktor.openapigen.model.security.APIKeyLocation
import com.papsign.ktor.openapigen.model.security.HttpSecurityScheme
import com.papsign.ktor.openapigen.model.security.SecuritySchemeModel
import com.papsign.ktor.openapigen.model.security.SecuritySchemeType
import com.papsign.ktor.openapigen.model.server.ServerModel
import com.papsign.ktor.openapigen.modules.providers.AuthProvider
import com.papsign.ktor.openapigen.openAPIGen
import com.papsign.ktor.openapigen.route.apiRouting
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.auth.OpenAPIAuthenticatedRoute
import com.papsign.ktor.openapigen.route.path.auth.get
import com.papsign.ktor.openapigen.route.path.auth.principal
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.request.host
import io.ktor.server.request.port
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.reflect.KType

object TestServerWithJwtAuth {

    @JvmStatic
    fun main(args: Array<String>) {
        embeddedServer(Netty, 8080, "localhost") {
            testServerWithJwtAuth()
        }.start(true)
    }

    public fun Application.testServerWithJwtAuth() {
        //define basic OpenAPI info
        val authProvider = JwtProvider();
        val api = install(OpenAPIGen) {
            info {
                version = "0.1"
                title = "Test API"
                description = "The Test API"
                contact {
                    name = "Support"
                    email = "support@test.com"
                }
            }
            server("https://api.test.com/") {
                description = "Main production server"
            }
            addModules(authProvider)
            replaceModule(DefaultSchemaNamer, object : SchemaNamer {
                val regex = Regex("[A-Za-z0-9_.]+")
                override fun get(type: KType): String {
                    return type.toString().replace(regex) { it.value.split(".").last() }.replace(Regex(">|<|, "), "_")
                }
            })
        }

        install(ContentNegotiation) {
            jackson {
                enable(
                    DeserializationFeature.WRAP_EXCEPTIONS,
                    DeserializationFeature.USE_BIG_INTEGER_FOR_INTS,
                    DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
                )

                enable(SerializationFeature.WRAP_EXCEPTIONS, SerializationFeature.INDENT_OUTPUT)

                setSerializationInclusion(JsonInclude.Include.NON_NULL)

                setDefaultPrettyPrinter(DefaultPrettyPrinter().apply {
                    indentArraysWith(DefaultPrettyPrinter.FixedSpaceIndenter.instance)
                    indentObjectsWith(DefaultIndenter("  ", "\n"))
                })

                registerModule(JavaTimeModule())
            }
        }

        install(Authentication) {
            installJwt(this)
        }

        // serve OpenAPI and redirect from root
        routing {
            get("/openapi.json") {
                val host = ServerModel(
                    call.request.origin.scheme + "://" + call.request.host() + if (setOf(
                            80,
                            443
                        ).contains(call.request.port())
                    ) "" else ":${call.request.port()}"
                )
                call.application.openAPIGen.api.servers.add(0, host)
                call.respond(call.application.openAPIGen.api.serialize())
                call.application.openAPIGen.api.servers.remove(host)
            }

            get("/") {
                call.respondRedirect("/swagger-ui/index.html?url=/openapi.json", true)
            }
        }

        apiRouting {
            auth {
                get<StringParam, StringResponse, UserPrincipal>(
                    info("String Param Endpoint", "This is a String Param Endpoint"),
                    example = StringResponse("Hi")
                ) { params ->
                    val (userId, name) = principal()
                    respond(StringResponse("Hello $name, you submitted ${params.a}"))
                }
            }
        }
    }

    @Path("string/{a}")
    data class StringParam(@param:PathParam("A simple String Param") val a: String)

    @Response("A String Response")
    data class StringResponse(@param:Description("The string value") val str: String)

    val authProvider = JwtProvider();

    inline fun NormalOpenAPIRoute.auth(route: OpenAPIAuthenticatedRoute<UserPrincipal>.() -> Unit): OpenAPIAuthenticatedRoute<UserPrincipal> {
        val authenticatedKtorRoute = this.ktorRoute.authenticate { }
        val openAPIAuthenticatedRoute =
            OpenAPIAuthenticatedRoute(authenticatedKtorRoute, this.provider.child(), authProvider = authProvider);
        return openAPIAuthenticatedRoute.apply {
            route()
        }
    }

    data class UserPrincipal(val userId: String, val name: String?) : Principal

    class JwtProvider : AuthProvider<UserPrincipal> {
        override val security: Iterable<Iterable<AuthProvider.Security<*>>> =
            listOf(
                listOf(
                    AuthProvider.Security(
                        SecuritySchemeModel(
                            SecuritySchemeType.http,
                            scheme = HttpSecurityScheme.bearer,
                            bearerFormat = "JWT",
                            referenceName = "jwtAuth",
                        ), emptyList()
                    ),
                    AuthProvider.Security(
                        SecuritySchemeModel(
                            SecuritySchemeType.apiKey,
                            `in` = APIKeyLocation.cookie,
                            name = "ThisIsCookieName",
                            referenceName = "ThisIsSchemeName",
                        ), emptyList<Scopes>()
                    )
                )
            )

        override suspend fun getAuth(pipeline: RoutingContext): UserPrincipal {
            return pipeline.call.authentication.principal() ?: throw RuntimeException("No JWTPrincipal")
        }

        override fun apply(route: NormalOpenAPIRoute): OpenAPIAuthenticatedRoute<UserPrincipal> {
            val authenticatedKtorRoute = route.ktorRoute.authenticate { }
            return OpenAPIAuthenticatedRoute(authenticatedKtorRoute, route.provider.child(), this)
        }
    }

    enum class Scopes(override val description: String) : Described {
        Profile("Some scope")
    }

    val jwtRealm: String = "example-jwt-realm"
    val jwtIssuer: String = "http://localhost:9091/auth/realms/$jwtRealm"
    val jwtEndpoint: String = "$jwtIssuer/protocol/openid-connect/certs"

    fun installJwt(provider: AuthenticationConfig) {
        provider.apply {
            jwt {
                realm = jwtRealm
                verifier(getJwkProvider(jwtEndpoint), jwtIssuer)
                validate { credentials ->
                    UserPrincipal(
                        credentials.payload.subject,
                        credentials.payload.claims["name"]?.asString()
                    )
                }
            }
        }
    }

    private fun getJwkProvider(jwkEndpoint: String): JwkProvider {
        return JwkProviderBuilder(URL(jwkEndpoint))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()
    }

}
