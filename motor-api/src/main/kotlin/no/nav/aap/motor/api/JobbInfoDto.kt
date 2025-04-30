package no.nav.aap.motor.api

import no.nav.aap.motor.JobbStatus
import java.time.LocalDateTime

/**
 * @param type Jobbtype. Er et element fra runtime-versjonen av [no.nav.aap.motor.JobbType].
 * @param planlagtKjøretidspunkt Når er denne jobben planlagt å kjøre neste gang.
 * @param feilmelding Hvis jobben har feilet, den lagrede feilmeldingen.
 * @param beskrivelse Beskrivelsen til jobben, som returnert fra [no.nav.aap.motor.Jobb.beskrivelse].
 * @param navn Navnet til jobben, som returnert fra [no.nav.aap.motor.Jobb.navn].
 */
public data class JobbInfoDto(
    public val id: Long,
    public val type: String,
    public val status: JobbStatus,
    public val opprettetTidspunkt: LocalDateTime,
    public val planlagtKjøretidspunkt: LocalDateTime,
    public val metadata: Map<String, String>,
    public val antallFeilendeForsøk: Int = 0,
    public val feilmelding: String? = null,
    public val beskrivelse: String,
    public val navn: String
)