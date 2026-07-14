import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.server.commonKtorModule
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.Motor
import no.nav.aap.motor.api.JobbInfoDto
import no.nav.aap.motor.api.motorApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Date
import javax.sql.DataSource

class MotorApiTest {

    @AutoClose
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun teardown() {
        dataSource.close()
    }

    @Test
    fun `skal ikke krasje på ingen planlagte jobber`() {
        val motor = Motor(dataSource, jobber = listOf(TøysTestJobbUtfører))

        motor.start()

        testApplication {
            application { module(dataSource) }

            val response = client.get("/drift/api/jobb/planlagte-jobber")

            assertThat(response.status.value).isEqualTo(200)
        }

        motor.stop()
    }

    @Test
    fun `skal gi 403 når bruker mangler påkrevd rolle`() {
        testApplication {
            application { moduleWithRoller(dataSource, godkjenteRoller = listOf(PÅKREVD_TESTROLLE)) }

            val response = client.get("/drift/api/jobb/planlagte-jobber") {
                bearerAuth(lagTestToken(grupper = listOf("annen-gruppe")))
            }

            assertThat(response.status.value).isEqualTo(403)
        }
    }

    @Test
    fun `skal gi 200 når bruker har påkrevd rolle`() {
        testApplication {
            application { moduleWithRoller(dataSource, godkjenteRoller = listOf(PÅKREVD_TESTROLLE)) }

            val response = client.get("/drift/api/jobb/planlagte-jobber") {
                bearerAuth(lagTestToken(grupper = listOf(PÅKREVD_TESTROLLE)))
            }

            assertThat(response.status.value).isEqualTo(200)
        }
    }

    @Test
    fun `kan hente opprettet tidspunkt i listen over kjørte jobber`() {
        val motor = Motor(dataSource, jobber = listOf(TøysTestJobbUtfører))

        dataSource.transaction {
            FlytJobbRepositoryImpl(it).leggTil(JobbInput(TøysTestJobbUtfører))
        }

        motor.start()

        testApplication {
            application { module(dataSource) }

            val response = client.get("/drift/api/jobb/sisteKjørte")

            assertThat(response.status.value).isEqualTo(200)
            val body = response.bodyAsText()
            val jobbInfoDto = DefaultJsonMapper.fromJson< List<JobbInfoDto>>(body)
            assertThat(jobbInfoDto).hasSize(1)
            assertThat(jobbInfoDto.first().navn).isEqualTo("tøys")
            assertThat(jobbInfoDto.first().opprettetTidspunkt).isNotNull()
        }

        motor.stop()
    }
}

fun Application.module(dataSource: DataSource) {
    System.setProperty("azure.openid.config.token.endpoint", "http://localhost:1234/token")
    System.setProperty("azure.app.client.id", "behandlingsflyt")
    System.setProperty("azure.app.client.secret", "")
    System.setProperty("azure.openid.config.jwks.uri", "http://localhost:1234/jwks")
    System.setProperty("azure.openid.config.issuer", "behandlingsflyt")

    commonKtorModule(
        prometheus = SimpleMeterRegistry(),
        azureConfig = AzureConfig(),
        infoModel = InfoModel()
    )
    routing {
        apiRouting {
            motorApi(dataSource)
        }
    }
}

private const val PÅKREVD_TESTROLLE = "drift-gruppe"
private const val TEST_JWT_SECRET = "test-hemmelighet-som-er-minst-32-tegn-lang"
private const val TEST_JWT_AUDIENCE = "behandlingsflyt"
private const val TEST_JWT_ISSUER = "behandlingsflyt"

private fun lagTestToken(grupper: List<String>): String =
    JWT.create()
        .withClaim("groups", grupper)
        .withAudience(TEST_JWT_AUDIENCE)
        .withIssuer(TEST_JWT_ISSUER)
        .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000))
        .sign(Algorithm.HMAC256(TEST_JWT_SECRET))

fun Application.moduleWithRoller(dataSource: DataSource, godkjenteRoller: List<String>) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(DefaultJsonMapper.objectMapper(), true))
    }
    install(OpenAPIGen) {
        serveOpenApiJson = false
        serveSwaggerUi = false
    }
    install(Authentication) {
        jwt("azure") {
            verifier(
                JWT.require(Algorithm.HMAC256(TEST_JWT_SECRET))
                    .withAudience(TEST_JWT_AUDIENCE)
                    .withIssuer(TEST_JWT_ISSUER)
                    .build()
            )
            validate { cred -> JWTPrincipal(cred.payload) }
        }
    }
    routing {
        authenticate("azure") {
            apiRouting {
                motorApi(dataSource, godkjenteRoller)
            }
        }
    }
}

class TøysTestJobbUtfører() : JobbUtfører {

    override fun utfør(input: JobbInput) {
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return TøysTestJobbUtfører()
        }

        override fun type(): String {
            return "tøys"
        }

        override fun navn(): String {
            return type()
        }

        override fun beskrivelse(): String {
            return type()
        }
    }
}
