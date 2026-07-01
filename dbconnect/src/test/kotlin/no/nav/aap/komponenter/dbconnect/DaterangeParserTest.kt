package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month

internal class DaterangeParserTest {

    @Test
    fun `Konverterer Periode til lukket daterange`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.toSQL(Periode(fom, tom))

        assertThat(periode).isEqualTo("[$fom,$tom]")
    }

    @Test
    fun `Parser daterange der både fom og tom er lukket`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("[$fom,$tom]")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der fom er lukket og tom er åpen`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("[$fom,$tom)")

        assertThat(periode.fom).isEqualTo(fom)
        assertThat(periode.tom.plusDays(1)).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der fom er åpen og tom er lukket`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("($fom,$tom]")

        assertThat(periode.fom.minusDays(1)).isEqualTo(fom)
        assertThat(periode.tom).isEqualTo(tom)
    }

    @Test
    fun `Parser daterange der både fom og tom er åpne`() {
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusDays(10)
        val periode = DaterangeParser.fromSQL("($fom,$tom)")

        assertThat(periode.fom.minusDays(1)).isEqualTo(fom)
        assertThat(periode.tom.plusDays(1)).isEqualTo(tom)
    }

    @Test
    fun `Kan parse Tid MIN og Tid MAKS`() {
        val tidMin: LocalDate = LocalDate.of(1, Month.JANUARY, 1)
        val tidMaks: LocalDate = LocalDate.of(2999, Month.JANUARY, 1)

        val sqlPeriode = DaterangeParser.toSQL(Periode(tidMin, tidMaks))
        assertThat(sqlPeriode).isEqualTo("[$tidMin,$tidMaks]")

        val periode = DaterangeParser.fromSQL(sqlPeriode)
        assertThat(periode.fom).isEqualTo(tidMin)
        assertThat(periode.tom).isEqualTo(tidMaks)
    }

    @Test
    fun `Parser daterange der fom er 10_000`() {
        val periode = DaterangeParser.fromSQL("[2012-09-17,10000-12-31)")

        assertThat(periode.fom).isEqualTo(LocalDate.of(2012, 9, 17))
        assertThat(periode.tom.plusDays(1)).isEqualTo(LocalDate.of(10000, 12, 31))
    }
}
