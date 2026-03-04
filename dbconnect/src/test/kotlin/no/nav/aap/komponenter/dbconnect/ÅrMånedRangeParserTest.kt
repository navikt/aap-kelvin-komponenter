package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.ÅrMånedPeriode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

internal class ÅrMånedRangeParserTest {

    @Test
    fun `Konverterer ÅrMånedPeriode til lukket månedrange`() {
        val fom = YearMonth.of(2024, 1)
        val tom = YearMonth.of(2024, 10)
        val årMånedPeriode = ÅrMånedRangeParser.toSQL(ÅrMånedPeriode(fom, tom))

        assertThat(årMånedPeriode).isEqualTo("[2024-01,2024-10]")
    }

    @Test
    fun `Parser månedrange der både fom og tom er lukket`() {
        val årMånedPeriode = ÅrMånedRangeParser.fromSQL("[2024-01,2024-10]")

        assertThat(årMånedPeriode.fom).isEqualTo(YearMonth.of(2024, 1))
        assertThat(årMånedPeriode.tom).isEqualTo(YearMonth.of(2024, 10))
    }

    @Test
    fun `Parser månedrange der fom er åpen`() {
        val årMånedPeriode = ÅrMånedRangeParser.fromSQL("(2024-01,2024-10]")

        assertThat(årMånedPeriode.fom).isEqualTo(YearMonth.of(2024, 2))
        assertThat(årMånedPeriode.tom).isEqualTo(YearMonth.of(2024, 10))
    }

    @Test
    fun `Parser månedrange der tom er åpen`() {
        val årMånedPeriode = ÅrMånedRangeParser.fromSQL("[2024-01,2024-10)")

        assertThat(årMånedPeriode.fom).isEqualTo(YearMonth.of(2024, 1))
        assertThat(årMånedPeriode.tom).isEqualTo(YearMonth.of(2024, 9))
    }

    @Test
    fun `Round-trip test`() {
        val original = ÅrMånedPeriode(YearMonth.of(2020, 1), YearMonth.of(2025, 12))
        val sql = ÅrMånedRangeParser.toSQL(original)
        val parsed = ÅrMånedRangeParser.fromSQL(sql)

        assertThat(parsed).isEqualTo(original)
    }
}
