package no.nav.aap.komponenter.dbtest

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import javax.sql.DataSource

public object InitTestDatabase {
    public val dataSource: DataSource
    private var flyway: Flyway

    init {
        // Postgres 15 korresponderer til versjon i nais.yaml
        val postgres = PostgreSQLContainer<_>("postgres:16")
        postgres.start()
        val jdbcUrl = postgres.jdbcUrl
        val username = postgres.username
        val password = postgres.password
        dataSource = HikariDataSource(HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            this.username = username
            this.password = password
            minimumIdle = 1
            initializationFailTimeout = 30000
            idleTimeout = 10000
            connectionTimeout = 10000
            maxLifetime = 900000
            connectionTestQuery = "SELECT 1"

            /* Postgres i GCP kjører med UTC som timezone. Testcontainers-postgres
            * vil bruke samme timezone som maskinen den kjører fra (Europe/Oslo). Så
            * for å kunne teste at implisitte konverteringer mellom database og jvm blir riktig
            * så settes postgres opp som i gcp. */
            connectionInitSql = "SET TIMEZONE TO 'UTC'"
        })

        flyway = Flyway
            .configure()
            .cleanDisabled(false)
            .cleanOnValidationError(true)
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()
        migrate()
    }

    public fun migrate() {
        flyway.migrate()
    }

    public fun clean() {
        flyway.clean()
    }
}
