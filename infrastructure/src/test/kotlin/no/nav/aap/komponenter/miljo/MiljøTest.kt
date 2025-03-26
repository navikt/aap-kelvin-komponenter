package no.nav.aap.komponenter.miljo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class MiljøTest {

    @Test
    fun `er prod hvis miljøvariable ikke er satt`() {
        System.clearProperty(ENV_VAR_NAME)

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.PROD)

        assertThat(Miljø.erProd()).isTrue
    }


    @Test
    fun `er prod`() {
        System.clearProperty("prod-gcp")

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.PROD)

        assertThat(Miljø.erProd()).isTrue
    }

    @Test
    fun `er dev`() {
        System.setProperty(ENV_VAR_NAME, "dev-gcp")

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.DEV)

        assertThat(Miljø.erDev()).isTrue
    }

    @Test
    fun `er lokal`() {
        System.setProperty(ENV_VAR_NAME, "LOCAL")

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.LOKALT)

        assertThat(Miljø.erLokal()).isTrue
    }
}
