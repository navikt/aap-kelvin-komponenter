package no.nav.aap.komponenter.server

import com.papsign.ktor.openapigen.model.info.InfoModel
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson3.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.server.auth.IdentityProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode

class CommonKtorModuleKtTest {

    @Test
    fun `adding title to openapi spec`() {
        // Texas
        System.setProperty("nais.token.endpoint", "http://localhost:1234/token")
        System.setProperty("nais.token.exchange.endpoint", "http://localhost:1234/token/exchange")
        System.setProperty("nais.token.introspection.endpoint", "http://localhost:1234/introspect")

        var openApiJSON: String? = null

        testApplication {
            application {
                val answ = commonKtorModule(
                    prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    infoModel = InfoModel(title = "My cute title"),
                    identityProvider = IdentityProvider.ENTRA_ID
                )

                openApiJSON = answ.getOpenApiJSON()
            }

            val client = createClient {
                install(ContentNegotiation) {
                    register(
                        ContentType.Application.Json,
                        JacksonConverter(objectMapper = DefaultJsonMapper.objectMapper(), true)
                    )
                }
            }

            val resp = client.get("/swagger-ui/index.html")
            assertThat(resp.status.value).isEqualTo(200)

            val respJson = client.get("/openapi.json")
            assertThat(respJson.status.value).isEqualTo(200)

            val respJsonObj = respJson.body<JsonNode>()

            val openApiTitle = respJsonObj.get("info").get("title").asText()

            assertThat(openApiTitle).isEqualTo("My cute title")
            assertThat(openApiJSON).isNotNull()
            assertThat(openApiJSON?.length).isGreaterThan(10)
        }
    }
}