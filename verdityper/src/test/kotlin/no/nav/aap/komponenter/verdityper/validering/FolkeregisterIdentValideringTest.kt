package no.nav.aap.komponenter.verdityper.validering

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class FolkeregisterIdentValideringTest {
    @Test
    fun `Sjekk gyldige syntetiske fnr`() {
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("22128209852")).isTrue
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("04438346142")).isTrue
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("16418922846")).isTrue
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("30481381246")).isTrue
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("04458936760")).isTrue
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("30481381246")).isTrue
    }

    @Test
    fun `Sjekk ugyldige numeriske verdier`() {
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("1234")).isFalse

        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("00000000000")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("11111111111")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("22222222222")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("33333333333")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("44444444444")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("55555555555")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("66666666666")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("77777777777")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("88888888888")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("99999999999")).isFalse

        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("36117512737")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("12345678901")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("00000000001")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("10000000000")).isFalse
    }

    @Test
    fun `Sjekk ugyldige tekst verdier`() {
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("hei")).isFalse
        Assertions.assertThat(FolkeregisterIdentValidering.erGyldig("gyldigfnrmedtekst11057523044")).isFalse
    }
}
