package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.postgresql.PostgreSQLContainer
import java.io.PrintWriter
import java.sql.Connection
import java.sql.ConnectionBuilder
import java.sql.ShardingKeyBuilder
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Logger
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds
import kotlin.use

/**
 * Oppretter en ny database som kan brukes i tester.
 * Databasen kan brukes parallelt med andre tester.
 *
 * Bruk slik:
 * <pre><code>
 * @BeforeAll
 * @JvmStatic
 * fun setup() {
 * dataSource = TestDataSource()
 * }
 *
 * @AfterAll
 * @JvmStatic
 * fun tearDown() = dataSource.close()
 * </code></pre>
 *
 * Eller dersom du trenger å nullstille databasens innhold mellom hver test:
 * <pre><code>
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
        private val currentDatabaseNumber = AtomicInteger(1)
        private val logger = LoggerFactory.getLogger(TestDataSource::class.java)
        private const val MAX_CONNECTIONS_COUNT = 100 // Postgres default

        // Postgres 16 korresponderer til versjon i nais.yaml
        private val postgres: PostgreSQLContainer = PostgreSQLContainer("postgres:16")
            .withDatabaseName(templateDb)
            .withLogConsumer(Slf4jLogConsumer(logger))
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(60))
            .withCommand("postgres",
                "-c", "work_mem=8MB", // default 4MB, økt pga mange parallelle tester
                "-c", "shared_buffers=256MB", // default 128MB, 1.2GB i Dev-GCP
        )

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
            val databaseName = "test${currentDatabaseNumber.getAndIncrement()}"
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
                initializationFailTimeout = 5.seconds.inWholeSeconds
                connectionTimeout = 5.seconds.inWholeMilliseconds
                connectionTestQuery = "SELECT 1"
                dataSourceProperties.putAll(
                    mapOf(
                        "logUnclosedConnections" to true, // vår kode skal lukke alle connections
                        "assumeMinServerVersion" to "16.0" // raskere oppstart av driver
                    )
                )

                maximumPoolSize = MAX_CONNECTIONS_COUNT

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

public fun TestDataSource.clear() {
    this.connection.use { conn ->
        // Tøm alle tabeller unntatt flyway sine
        conn.prepareStatement(
            """
                DO $$
                DECLARE
                    r RECORD;
                BEGIN
                    FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename NOT LIKE 'flyway_%') LOOP
                        EXECUTE 'TRUNCATE TABLE public.' || quote_ident(r.tablename) || ' RESTART IDENTITY CASCADE';
                    END LOOP;
                END;
                $$;
            """.trimIndent()
        ).use { it.execute() }
    }
}