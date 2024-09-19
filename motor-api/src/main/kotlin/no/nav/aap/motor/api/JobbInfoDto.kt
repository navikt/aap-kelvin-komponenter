package no.nav.aap.motor.api

import no.nav.aap.motor.JobbStatus
import java.time.LocalDateTime

public class JobbInfoDto(
    public val id: Long,
    public val type: String,
    public val status: JobbStatus,
    public val planlagtKjøretidspunkt: LocalDateTime,
    public val metadata: Map<String, String>,
    public val antallFeilendeForsøk: Int = 0,
    public val feilmelding: String? = null,
    public val beskrivelse: String,
    public val navn: String
)