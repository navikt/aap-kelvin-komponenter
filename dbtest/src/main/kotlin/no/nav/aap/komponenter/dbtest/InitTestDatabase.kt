package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

public object InitTestDatabase {
    private const val clerkDatabase = "clerk"
    private val databaseNumber = AtomicInteger()

    // Postgres 16 korresponderer til versjon i nais.yaml
    private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer<_>("postgres:16")
        .withDatabaseName(clerkDatabase)

    private val clerkDataSource: DataSource
    private var flyway: FlywayOps

    private val dataSource: DataSource

    init {
        postgres.start()
        clerkDataSource = newDataSource("clerk")

        val templateDataSource = newDataSource("template1")
        flywayFor(templateDataSource)
            .migrate()
        templateDataSource.close()

        dataSource = freshDatabase()
        flyway = flywayFor(dataSource)
    }

    public fun freshDatabase(): DataSource {
        val databaseName = "test${databaseNumber.getAndIncrement()}"
        clerkDataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.executeUpdate("create database $databaseName template template1")
            }
        }
        println("Startet fresh Postgres med URL ${postgres.jdbcUrl}. Brukernavn: ${postgres.username}. Passord: ${postgres.password}. Db-navn: $databaseName")
        return newDataSource(databaseName)
    }

    public interface FlywayOps {
        public fun migrate()
        public fun clean()
    }

    public fun flywayFor(dataSource: DataSource): FlywayOps {
        return object : FlywayOps {
            private val flyway = Flyway
                .configure()
                .cleanDisabled(false)
                .dataSource(dataSource)
                .locations("flyway")
                .validateMigrationNaming(true)
                .load()

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
            minimumIdle = 1

            /* Postgres i GCP kjører med UTC som timezone. Testcontainers-postgres
            * vil bruke samme timezone som maskinen den kjører fra (Europe/Oslo). Så
            * for å kunne teste at implisitte konverteringer mellom database og jvm blir riktig
            * så settes postgres opp som i gcp. */
            connectionInitSql = "SET TIMEZONE TO 'UTC'"
        })
    }

    public fun migrate() {
        flyway.migrate()
    }

    public fun clean() {
        flyway.clean()
    }
}
