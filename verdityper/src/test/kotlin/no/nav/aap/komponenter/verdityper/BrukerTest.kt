package no.nav.aap.komponenter.verdityper

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BrukerTest {
    @Test
    fun `serialization and deserialization roundtrip`() {
        val bruker = Bruker("Z12345")
        val json = DefaultJsonMapper.toJson(bruker)
        assertThat(json).isEqualTo("\"Z12345\"")

        val deserialized = DefaultJsonMapper.fromJson<Bruker>(json)
        assertThat(deserialized).isEqualTo(bruker)
        assertThat(deserialized.ident).isEqualTo("Z12345")
    }
}