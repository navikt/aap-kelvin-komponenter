package no.nav.aap.komponenter.verdityper

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class TidspunktTest {
    @Test
    fun `roundtrip json`() {
        val tidspunkt = Tidspunkt.parse("2021-04-24T13:43:02.123456Z")

        val json = DefaultJsonMapper.toJson(tidspunkt)
        assertThat(json).isEqualTo("\"2021-04-24T13:43:02.123456Z\"")

        val deserialized = DefaultJsonMapper.fromJson<Tidspunkt>(json)

        assertThat(deserialized).isEqualTo(tidspunkt)
        assertThat(deserialized.asInstant)
            .isEqualTo(Instant.parse("2021-04-24T13:43:02.123456Z"))
    }

    @Test
    fun `trunkerer nanosekunder`() {
        val instant = Instant.parse("2021-04-24T13:43:02.123456789Z")
        val tidspunkt = Tidspunkt.ofInstant(instant)

        assertThat(tidspunkt)
            .isEqualTo(Tidspunkt.parse("2021-04-24T13:43:02.123456Z"))
    }

    @Test
    fun `instant som kun er forskjellige i nanosekunder blir like Tidspunkt`() {
        val instant1 = Instant.parse("2021-04-24T13:43:02.123456111Z")
        val instant2 = Instant.parse("2021-04-24T13:43:02.123456222Z")
        assertThat(Tidspunkt.ofInstant(instant1))
            .isEqualTo(Tidspunkt.ofInstant(instant2))
    }
}