package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

public object InitTestDatabase : Closeable {
    private const val clerkDatabase = "clerk"
    private val databaseNumber = AtomicInteger()
    private val logger: Logger = LoggerFactory.getLogger(InitTestDatabase::class.java)

    // Postgres 16 korresponderer til versjon i nais.yaml
    @JvmStatic
    private val postgres: PostgreSQLContainer<*> =
        PostgreSQLContainer<_>("postgres:16").withDatabaseName(clerkDatabase).withLogConsumer(Slf4jLogConsumer(logger))
            .waitingFor(Wait.forListeningPort())

    private val clerkDataSource: DataSource
    private var flyway: FlywayOps

    private val dataSource: DataSource

    init {
        postgres.start()
        clerkDataSource = newDataSource("clerk")

        val templateDataSource = newDataSource("template1")
        flywayFor(templateDataSource).migrate()
        templateDataSource.close()

        dataSource = freshDatabase()
        flyway = flywayFor(dataSource)
    }

    public fun freshDatabase(): DataSource {
        val databaseName = "test${databaseNumber.getAndIncrement()}"
        val freshUrl = synchronized(clerkDataSource) {
            clerkDataSource.connection.use { connection ->
                connection.createStatement().use { stmt ->
                    stmt.executeUpdate("create database $databaseName template template1")
                }
            }
            (clerkDataSource as HikariConfig).jdbcUrl
        }
        println("Startet fresh Postgres med URL $freshUrl. Brukernavn: ${postgres.username}. Passord: ${postgres.password}. Db-navn: $databaseName")
        return newDataSource(databaseName)
    }

    public interface FlywayOps {
        public fun migrate()
        public fun clean()
    }

    public fun flywayFor(dataSource: DataSource): FlywayOps {
        return object : FlywayOps {
            private val flyway = Flyway.configure().cleanDisabled(false).dataSource(dataSource).locations("flyway")
                .validateMigrationNaming(true).load()

            override fun migrate() {
                flyway.migrate()
            }

            override fun clean() {
                flyway.clean()
            }
        }
    }

    private fun newDataSource(dbname: String): HikariDataSource {
        return HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = postgres.jdbcUrl.replace(clerkDatabase, dbname)
            this.username = postgres.username
            this.password = postgres.password
            initializationFailTimeout = 10000
            idleTimeout = 600000
            connectionTimeout = 30000
            maxLifetime = 1800000
            connectionTestQuery = "SELECT 1"
            dataSourceProperties.putAll(
                mapOf(
                    "logUnclosedConnections" to true, // vår kode skal lukke alle connections
                    "assumeMinServerVersion" to "16.0" // raskere oppstart av driver
                )
            )

            minimumIdle = 1

            /* Postgres i GCP kjører med UTC som timezone. Testcontainers-postgres
            * vil bruke samme timezone som maskinen den kjører fra (Europe/Oslo). Så
            * for å kunne teste at implisitte konverteringer mellom database og jvm blir riktig
            * så settes postgres opp som i gcp. */
            connectionInitSql = "SET TIMEZONE TO 'UTC'"
        })
    }

    public fun migrate() {
        synchronized(flyway) {
            flyway.migrate()
        }
    }

    public fun clean() {
        synchronized(flyway) {
            flyway.clean()
        }
    }

    override fun close() {
        synchronized(postgres) {
            postgres.stop()
        }
    }

    public fun closerFor(dataSource: DataSource) {
        try {
            // Close kan feile hvis feks. testcontainer ikke klarte å starte opp
            (dataSource as HikariDataSource).close()
        } finally {
            // ignorert
        }
    }
}
