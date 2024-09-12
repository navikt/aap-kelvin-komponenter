package no.nav.aap.komponenter.miljo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class MiljøTest {

    @Test
    fun `er prod hvis miljøvariable ikke er satt`() {
        System.clearProperty(ENV_VAR_NAME)

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.PROD)
    }

    @Test
    fun `er dev`() {
        System.setProperty(ENV_VAR_NAME, "DEV-")

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.DEV)
    }

    @Test
    fun `er lokal`() {
        System.setProperty(ENV_VAR_NAME, "LOCAL")

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.LOKALT)
    }

}