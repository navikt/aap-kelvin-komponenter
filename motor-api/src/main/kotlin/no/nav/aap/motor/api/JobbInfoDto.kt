package no.nav.aap.motor.api

import no.nav.aap.motor.JobbStatus
import java.time.LocalDateTime

class JobbInfoDto(
    val id: Long,
    val type: String,
    val status: JobbStatus,
    val planlagtKjøretidspunkt: LocalDateTime,
    val metadata: Map<String, String>,
    val antallFeilendeForsøk: Int = 0,
    val feilmelding: String? = null,
    val beskrivelse: String,
    val navn: String
)