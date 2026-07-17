package no.nav.aap.motor

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
public data class JobbTilleggsinfo(
    public val ansvarlig: String? = null,
    public val kommentarer: List<Kommentar> = emptyList(),
) {
    public fun leggTilKommentar(kommentar: Kommentar): JobbTilleggsinfo {
        return this.copy(kommentarer = this.kommentarer + kommentar)
    }

    public fun settAnsvarlig(ansvarlig: String?): JobbTilleggsinfo {
        return this.copy(ansvarlig = ansvarlig?.ifBlank { null })
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class Kommentar(
    public val skrevetAv: String,
    public val tekst: String,
    public val tidspunkt: LocalDateTime,
) {
    public companion object {
        public fun ny(tekst: String, skrevetAv: String): Kommentar = Kommentar(skrevetAv, tekst, LocalDateTime.now())
    }
}
