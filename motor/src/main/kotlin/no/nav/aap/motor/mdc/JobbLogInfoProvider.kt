package no.nav.aap.motor.mdc

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput

public interface JobbLogInfoProvider {

    public fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon?
}
