package no.nav.aap.komponenter.verdityper

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GUnitTest {
    @Test
    fun `equals sammenligner på verdi virker`() {
        val a = GUnit("1")
        val b = GUnit(BigDecimal("1"))
        assertEquals(a, b)
    }

    @Test
    fun `serialisere og deserialisere`() {
        val gUnit = GUnit("100.00")
        val json = DefaultJsonMapper.toJson(gUnit)

        val deserialisertGUnit = DefaultJsonMapper.fromJson<GUnit>(json)
        assertThat(deserialisertGUnit).isEqualTo(gUnit)
        assertThat(json).isEqualTo("100.0000000000")
    }

    @Test
    fun `deserialisere liste av g-units`() {
        val liste = """
            [123.0, 54.0]
        """.trimIndent()

        val deserialisertListe = DefaultJsonMapper.fromJson<List<GUnit>>(liste)
        assertThat(deserialisertListe).containsExactly(GUnit("123.0"), GUnit("54.0"))
    }
}