package no.nav.aap.motor.mdc

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput

interface JobbLogInfoProvider {

    fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon?
}
