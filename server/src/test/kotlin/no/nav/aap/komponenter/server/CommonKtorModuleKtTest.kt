package no.nav.aap.komponenter.server

import com.fasterxml.jackson.databind.JsonNode
import com.papsign.ktor.openapigen.model.info.InfoModel
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommonKtorModuleKtTest {

    @Test
    fun `adding title to openapi spec`() {
        System.setProperty("azure.openid.config.token.endpoint", "http://localhost:1234/token")
        System.setProperty("azure.app.client.id", "behandlingsflyt")
        System.setProperty("azure.app.client.secret", "")
        System.setProperty("azure.openid.config.jwks.uri", "http://localhost:1234/jwks")
        System.setProperty("azure.openid.config.issuer", "behandlingsflyt")

        testApplication {
            application {
                commonKtorModule(
                    prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
                    azureConfig = AzureConfig(),
                    infoModel = InfoModel(title = "My cute title")
                )
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
        }
    }
}