package no.nav.aap.motor

import java.time.LocalDateTime


public data class JobbInputMedHistorikk(
    val jobb: JobbInput,
    val historikk: List<JobbHistorikk>
)

public data class JobbHistorikk(
    val jobbId: Long,
    val status: JobbStatus,
    val opprettet: LocalDateTime,
    val feilmelding: String?,
)