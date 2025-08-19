package no.nav.aap.komponenter.verdityper.validering

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class OrganisasjonsnummerValideringTest {
    @Test
    fun `Sjekk gyldige orgnr`() {
        assertThat(OrganisasjonsnummerValidering.erGyldig("958935420")).isTrue
        assertThat(OrganisasjonsnummerValidering.erGyldig("952857991")).isTrue
        assertThat(OrganisasjonsnummerValidering.erGyldig("627438400")).isTrue
        assertThat(OrganisasjonsnummerValidering.erGyldig("856776263")).isTrue
        assertThat(OrganisasjonsnummerValidering.erGyldig("276154117")).isTrue
        assertThat(OrganisasjonsnummerValidering.erGyldig("050069281")).isTrue
    }

    @Test
    fun `Sjekk div ugyldige orgnr`() {
        assertThat(OrganisasjonsnummerValidering.erGyldig("123456789")).isFalse
        assertThat(OrganisasjonsnummerValidering.erGyldig("000000000")).isFalse
        assertThat(OrganisasjonsnummerValidering.erGyldig("999888777")).isFalse
        assertThat(OrganisasjonsnummerValidering.erGyldig("987654321")).isFalse
        assertThat(OrganisasjonsnummerValidering.erGyldig("98765432a")).isFalse

        assertThat(OrganisasjonsnummerValidering.erGyldig("gorgon123")).isFalse
        assertThat(OrganisasjonsnummerValidering.erGyldig("")).isFalse
    }
}
