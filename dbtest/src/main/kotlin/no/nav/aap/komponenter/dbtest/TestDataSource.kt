package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.PrintWriter
import java.sql.Connection
import java.sql.ConnectionBuilder
import java.sql.ShardingKeyBuilder
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * Oppretter en ny database som kan brukes i tester.
 * Databasen kan brukes parallelt med andre tester.
 *
 * Bruk slik:
 * <pre><code>
 * @AutoClose
 * private val dataSource = TestDataSource()
 * </code></pre>
 *
 * Eller dersom du trenger å nullstille databasens innhold mellom hver test:
 * <pre><code>
 * @AutoClose
 * private lateinit var dataSource: TestDataSource
 *
 * @BeforeEach
 * fun setup() {
 *   dataSource = TestDataSource()
 * }
 *
 * @AfterEach
 * fun tearDown() {
 *   dataSource.close()
 * }
 *   ...
 * </code></pre>
 */
public class TestDataSource : AutoCloseable, DataSource {

    // Postgres serveren startes en gang, og migrasjonen av databasen kjøres en gang.
    // Den migrerte databasen lagres som en template, som brukes til å opprette en ny db for hver instans av TestDataSource.
    public companion object {
        private const val templateDb = "template1"
        private val databaseNumber = AtomicInteger()
        private val logger = LoggerFactory.getLogger(InitTestDatabase::class.java)

        // Postgres 16 korresponderer til versjon i nais.yaml
        private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer<_>("postgres:16")
            .withDatabaseName(templateDb)
            .withLogConsumer(Slf4jLogConsumer(logger))
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))

        // clerkDatasource brukes bare til CREATE DATABASE
        // Den opprettes lazy slik at vi unngår å starte postgres-containeren under initializing av TestDataSource
        private val clerkDatasource by lazy {
            postgres.start()

            // Migrer template-databasen som brukes som mal for alle testdatabaser
            newDatasource(templateDb).use { ds ->
                applyFlywayMigrate(ds)
            }

            newDatasource("postgres")
        }

        public fun freshDatabase(): HikariDataSource {
            val databaseName = "test${databaseNumber.getAndIncrement()}"
            clerkDatasource.connection.use { connection ->
                connection.createStatement().use { stmt ->
                    stmt.executeUpdate("CREATE DATABASE $databaseName TEMPLATE $templateDb")
                }
            }
            return newDatasource(databaseName)
        }

        private fun applyFlywayMigrate(dataSource: DataSource) {
            Flyway.configure()
                .dataSource(dataSource)
                .locations("flyway")
                .cleanDisabled(false)
                .validateMigrationNaming(true)
                .load()
                .migrate()
        }

        private fun newDatasource(dbName: String): HikariDataSource {
            val ds = HikariDataSource(HikariConfig().apply {
                jdbcUrl = postgres.jdbcUrl.replace(templateDb, dbName)
                username = postgres.username
                password = postgres.password
                initializationFailTimeout = 10_000
                idleTimeout = 600_000
                connectionTimeout = 30_000
                maxLifetime = 1_800_000
                connectionTestQuery = "SELECT 1"
                dataSourceProperties.putAll(
                    mapOf(
                        "logUnclosedConnections" to true, // vår kode skal lukke alle connections
                        "assumeMinServerVersion" to "16.0" // raskere oppstart av driver
                    )
                )

                minimumIdle = 1

                maximumPoolSize = 128 // Høy for å støtte parallelle tester

                /* Postgres i GCP kjører med UTC som timezone. Testcontainers-postgres
                * vil bruke samme timezone som maskinen den kjører fra (Europe/Oslo). Så
                * for å kunne teste at implisitte konverteringer mellom database og jvm blir riktig
                * så settes postgres opp som i gcp. */
                connectionInitSql = "SET TIMEZONE TO 'UTC'"
            })
            logger.debug(
                "Skapte tom Postgres-db med URL ${ds.jdbcUrl}. " + "Brukernavn: ${postgres.username}. " + "Passord: ${postgres.password}. Db-navn: $dbName"
            )
            return ds
        }
    }

    // by lazy slik at vi unngår å starte testcontainer under class initialization
    private val delegate: HikariDataSource by lazy { freshDatabase() }

    // Send videre alle kall til den ekte datasourcen:

    override fun getConnection(): Connection = delegate.connection

    override fun getConnection(username: String, password: String): Connection =
        delegate.getConnection(username, password)

    override fun getLogWriter(): PrintWriter = delegate.logWriter

    override fun setLogWriter(out: PrintWriter) {
        delegate.logWriter = out
    }

    override fun setLoginTimeout(seconds: Int) {
        delegate.loginTimeout = seconds
    }

    override fun getLoginTimeout(): Int = delegate.loginTimeout

    override fun createConnectionBuilder(): ConnectionBuilder {
        return delegate.createConnectionBuilder()
    }

    override fun getParentLogger(): Logger? = delegate.parentLogger

    override fun createShardingKeyBuilder(): ShardingKeyBuilder {
        return delegate.createShardingKeyBuilder()
    }

    public override fun close(): Unit = delegate.close()

    override fun <T : Any> unwrap(iface: Class<T>): T = delegate.unwrap(iface)

    override fun isWrapperFor(iface: Class<*>): Boolean = delegate.isWrapperFor(iface)

}
