import com.papsign.ktor.openapigen.model.info.InfoModel
import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.*
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
