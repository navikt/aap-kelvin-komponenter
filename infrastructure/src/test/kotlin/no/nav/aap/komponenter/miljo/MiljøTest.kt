package no.nav.aap.komponenter.miljo

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.spyk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class MiljøTest {

    init {
        mockkObject(EnvironmentVariableProvider)
    }

    @Test
    fun `er dev`() {
        every { EnvironmentVariableProvider.getEnv(any()) } returns "DEV-"

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.DEV)
    }

    @Test
    fun `er lokal`() {
        every { EnvironmentVariableProvider.getEnv(ENV_VAR_NAME) } returns "LOCAL"

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.LOKALT)
    }

    @Test
    fun `er prod hvis miljøvariable ikke er satt`() {
        every { EnvironmentVariableProvider.getEnv(ENV_VAR_NAME) } returns null

        val actual = Miljø.er()

        assertThat(actual).isEqualTo(MiljøKode.PROD)
    }
}