package no.nav.aap.komponenter.dbmigrering

import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.flywaydb.core.Flyway
import javax.sql.DataSource

public object Migrering {
    public fun migrate(dataSource: DataSource, cleanOnValidationError: Boolean = false) {
        val flyway = Flyway
            .configure()
            .cleanDisabled(Miljø.er() == MiljøKode.PROD)
            .cleanOnValidationError(cleanOnValidationError)
            .dataSource(dataSource)
            .locations("flyway")
            .validateMigrationNaming(true)
            .load()

        flyway.migrate()
    }
}