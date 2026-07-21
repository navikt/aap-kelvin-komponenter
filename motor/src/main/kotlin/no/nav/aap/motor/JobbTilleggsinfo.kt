package no.nav.aap.motor

import java.time.LocalDateTime

public data class JobbTilleggsinfo(
    public val kommentarer: List<Kommentar> = emptyList(),
)

public data class Kommentar(
    public val skrevetAv: String,
    public val tekst: String,
    public val tidspunkt: LocalDateTime,
) {
    public companion object {
        public fun ny(tekst: String, skrevetAv: String): Kommentar = Kommentar(skrevetAv, tekst, LocalDateTime.now())
    }
}
