package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

public object InitTestDatabase : Closeable {
    private const val templateDb = "template1"
    private val databaseNumber = AtomicInteger()
    private val logger = LoggerFactory.getLogger(InitTestDatabase::class.java)

    private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer<_>("postgres:16")
        .withDatabaseName(templateDb)
        .withLogConsumer(Slf4jLogConsumer(logger))
        .waitingFor(Wait.forListeningPort())
        .withStartupTimeout(java.time.Duration.ofSeconds(30))

    // Brukes bare til CREATE DATABASE
    private val clerkDatasource: DataSource

    init {
        postgres.start()

        // Migrer template-databasen som brukes som mal for alle testdatabaser
        newDatasource(templateDb).use { ds ->
            applyFlywayMigrate(ds)
        }

        clerkDatasource = newDatasource("postgres")
    }

    public fun freshDatabase(): DataSource {
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

    override fun close() {
        synchronized(postgres) {
            postgres.stop()
        }
    }

    public fun closerFor(dataSource: DataSource) {
        try {
            // Close kan feile hvis feks. testcontainer ikke klarte å starte opp
            (dataSource as HikariDataSource).close()
        } catch (_: Exception) {
            // ignorert
        }
    }
}