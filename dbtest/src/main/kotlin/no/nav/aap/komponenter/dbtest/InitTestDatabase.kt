package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.util.concurrent.atomic.AtomicInteger
import javax.sql.DataSource

@Suppress("DEPRECATION")
public object InitTestDatabase {
    private const val clerkDatabase = "clerk"
    private val databaseNumber = AtomicInteger()

    // Postgres 16 korresponderer til versjon i nais.yaml
    private val postgres: PostgreSQLContainer<*> = PostgreSQLContainer<_>("postgres:16")
        .withDatabaseName(clerkDatabase)

    private val clerkDataSource: DataSource
    private var flyway: Flyway

    @Deprecated("skaff deg din egen private og tomme database ved å kalle `InitTestDatabase.freshDatabase()`")
    public val dataSource: DataSource

    init {
        postgres.start()
        clerkDataSource = newDataSource("clerk")

        Flyway
            .configure()
            .cleanDisabled(false)
            .dataSource(newDataSource("template1"))
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()
            .migrate()


        dataSource = freshDatabase()
        flyway = Flyway
            .configure()
            .cleanDisabled(false)
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()

    }

    public fun freshDatabase(): DataSource {
        val databaseName = "test${databaseNumber.getAndIncrement()}"
        clerkDataSource.connection.use { connection ->
            connection.createStatement().use { stmt ->
                stmt.executeUpdate("create database $databaseName template template1")
            }
        }
        return newDataSource(databaseName)
    }

    private fun newDataSource(dbname: String): DataSource {
        return HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = postgres.jdbcUrl.replace("template1", dbname)
            this.username = postgres.username
            this.password = postgres.password
            minimumIdle = 1
            initializationFailTimeout = 5000
            idleTimeout = 600000
            connectionTimeout = 30000
            maxLifetime = 1800000
            connectionTestQuery = "SELECT 1"

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
