package no.nav.aap.komponenter.verdityper

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class GUnitTest {
    @Test
    fun `equals sammenligner på verdi virker`() {
        val a = GUnit("1")
        val b = GUnit(BigDecimal("1"))
        assertEquals(a, b)
    }
}