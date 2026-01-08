package no.nav.aap.komponenter.verdityper

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BeløpTest {

    /**
     * Påminner om muligheten for avrundingsfeil for utregninger med Beløp
     *
     * Eksempel regnestykke:
     * 1/300 + 1/300 = 2/300 = 0.0066666666 ≃ 0.01
     *
     * BigDecimal("0.0033333333") + BigDecimal("0.0033333333") = BigDecimal("0.0066666667") => Beløp("0.01")
     *      0.0033333333 + 0.0033333333 = 0.0066666666 ≃ 0.01
     *
     * IKKE: Beløp((Beløp("1.00").dividert(300)) + Beløp(Beløp("1.00").dividert(300)) = Beløp("0.00")
     *      0.00 + 0.00 = 0.00 ≃ 0.00
     *
     * Ved bruk av eksisterende Beløp.dividert(): BigDecimal som har default 10 desimaltall i utregning før verdi
     * wrappes til Beløp() igjen og avrundes med setScale(2, RoundingMode.HALF_UP) blir utregningen korrekt
     */
    @Test
    fun `divisjonsberegninger blir korrekt da ekstra presisjon benyttes i utregning og avrunding til 2 desimaler gjøres først for sluttresultat`() {
        val resultatMed10Desimaler = Beløp("1.00").dividert(Beløp("300.00"))
            .plus(
                Beløp("1.00").dividert(Beløp("300.00"))
            )
        val forventetMed10Desimaler = BigDecimal("0.0066666666")
        assertThat(resultatMed10Desimaler).isEqualTo(forventetMed10Desimaler)

        val resultatMed2Desimaler = Beløp(resultatMed10Desimaler)
        val forventetMed2Desimaler = Beløp("0.01")
        assertThat(resultatMed2Desimaler).isEqualTo(forventetMed2Desimaler)
    }

    /**
     * Konsumer av beløp.toTredjedeler() må eventuelt selv wrappe resultat i Beløp() som runder av til 2 desimaler for endelig sluttresultat
     */
    @Test
    fun `toTredjedeler beregnes uten avrundingsfeil da ekstra presisjon benyttes i utregning`() {
        val resultat = Beløp("0.01").toTredjedeler()
        assertThat(resultat).isEqualTo(BigDecimal("0.0066666667"))
    }

    @Test
    fun `heltallverdi avrunder og fjerne desimaler`() {
        val rundOpp = Beløp("250.50")
        assertThat(rundOpp.heltallverdi().toString()).isEqualTo("251")

        val rundNed = Beløp("250.49")
        assertThat(rundNed.heltallverdi().toString()).isEqualTo("250")
    }

    @Test
    fun `serialisere og deserialisere`() {
        val beløp = Beløp("100.00")
        val json = DefaultJsonMapper.toJson(beløp)

        val deserialisertBeløp = DefaultJsonMapper.fromJson<Beløp>(json)
        assertThat(deserialisertBeløp).isEqualTo(beløp)
        assertThat(json).isEqualTo("100.00")
    }

    @Test
    fun `deserialisere liste av beløp`() {
        val liste = """
            [123.0, 54.0]
        """.trimIndent()

        val deserialisertListe = DefaultJsonMapper.fromJson<List<Beløp>>(liste)
        assertThat(deserialisertListe).containsExactly(Beløp("123.0"), Beløp("54.0"))
    }

}