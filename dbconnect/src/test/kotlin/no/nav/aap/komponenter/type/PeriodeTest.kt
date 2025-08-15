package no.nav.aap.komponenter.type

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.Month
import java.time.Month.NOVEMBER

class PeriodeTest {

    @Test
    fun `teste validering - ingen feil`() {
        Periode(LocalDate.MIN, LocalDate.MAX)
        Periode(LocalDate.now(), LocalDate.now())
        Periode(LocalDate.now().minusDays(1), LocalDate.now())
    }

    @Test
    fun `teste validering - tom før fom`() {
        org.junit.jupiter.api.assertThrows<IllegalArgumentException> {
            Periode(LocalDate.MAX, LocalDate.MIN)
        }
    }

    @Test
    fun `teste overlapp - overlapper`() {
        val periode =
            Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(7))
        val periode2 = Periode(LocalDate.now().minusDays(8), LocalDate.now())

        assertThat(periode.overlapper(periode2)).isTrue()

        val periode1 = Periode(LocalDate.now(), LocalDate.now())
        val periode3 = Periode(LocalDate.now(), LocalDate.now())

        assertThat(periode1.overlapper(periode3)).isTrue()

        val periode4 = Periode(LocalDate.now().minusDays(8), LocalDate.now())
        val periode5 = Periode(LocalDate.now(), LocalDate.now().plusDays(8))

        assertThat(periode4.overlapper(periode5)).isTrue()
    }

    @Test
    fun `teste overlapp - overlapper ikke`() {
        val periode =
            Periode(LocalDate.now().minusDays(14), LocalDate.now().minusDays(7))
        val periode2 = Periode(LocalDate.now().minusDays(6), LocalDate.now())

        assertThat(periode.overlapper(periode2)).isFalse()
    }

    @Test
    fun `teste inneholder`() {
        val nå = LocalDate.now()
        val periode = Periode(nå.minusDays(4), nå.minusDays(2))

        assertThat(periode.inneholder(nå.minusDays(5))).isFalse()
        assertThat(periode.inneholder(nå.minusDays(4))).isTrue()
        assertThat(periode.inneholder(nå.minusDays(3))).isTrue()
        assertThat(periode.inneholder(nå.minusDays(2))).isTrue()
        assertThat(periode.inneholder(nå.minusDays(1))).isFalse()
    }

    @Test
    fun `teste inneholder periode`() {
        val nå = LocalDate.now()
        val periode = Periode(nå.minusDays(4), nå.minusDays(2))

        assertThat(periode.inneholder(Periode(nå.minusDays(5), nå.minusDays(3)))).isFalse()
        assertThat(periode.inneholder(Periode(nå.minusDays(4), nå.minusDays(3)))).isTrue()
        assertThat(periode.inneholder(Periode(nå.minusDays(4), nå.minusDays(2)))).isTrue()
        assertThat(periode.inneholder(Periode(nå.minusDays(3), nå.minusDays(2)))).isTrue()
        assertThat(periode.inneholder(Periode(nå.minusDays(3), nå.minusDays(1)))).isFalse()
    }

    @Test
    fun `teste utvid`() {
        val periode = Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(2))
        val periode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now().minusDays(4))
        val periode3 = Periode(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1))

        assertThat(periode.utvid(periode2)).isEqualTo(Periode(LocalDate.now().minusDays(5), LocalDate.now().minusDays(2)))
        assertThat(periode.utvid(periode3)).isEqualTo(Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(1)))
    }

    @Test
    fun `teste inntil`() {
        val periode = Periode(LocalDate.now().minusDays(4), LocalDate.now().minusDays(2))
        val periode2 = Periode(LocalDate.now().minusDays(6), LocalDate.now().minusDays(5))
        val periode3 = Periode(LocalDate.now().minusDays(5), LocalDate.now().minusDays(4))
        val periode4 = Periode(LocalDate.now().minusDays(1), LocalDate.now().minusDays(0))
        val periode5 = Periode(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1))

        assertThat(periode.inntil(periode2)).isTrue()
        assertThat(periode.inntil(periode3)).isFalse()
        assertThat(periode.inntil(periode4)).isTrue()
        assertThat(periode.inntil(periode5)).isFalse()
    }

    @Test
    fun `teste antallDager skuddår`() {
        val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))

        /* 2020 er et skuddår */
        assertThat(periode.antallDager()).isEqualTo(367)
    }
    @Test
    fun `teste antallDager vanlig år`() {
        val periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 1))

        /* 2020 er et skuddår */
        assertThat(periode.antallDager()).isEqualTo(366)
    }

    @Test
    fun `iterer over periode på 1 dag`() {
        val periode = Periode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 1, 1)
        )

        assertThat(periode.dager().toList()).isEqualTo(listOf(LocalDate.of(2020, 1, 1)))
    }

    @Test
    fun `iterer over periode på flere dager`() {
        val periode = Periode(
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 1, 3)
        )

        val forventetDager = (1..3).map { LocalDate.of(2020, 1, it) }
        assertThat(periode.dager().toList()).isEqualTo(forventetDager)
    }

    @Test
    fun `antall hverdager i periode`() {
        val periode = Periode(
            LocalDate.of(2024, NOVEMBER, 6),
            LocalDate.of(2024, NOVEMBER, 22),
        )

        assertThat(periode.antallDager(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)).isEqualTo(13)
    }

    @Test
    fun `flytte periode`() {
        val periode = Periode(
            LocalDate.of(2024, NOVEMBER, 6),
            LocalDate.of(2024, NOVEMBER, 22),
        )

        val res = periode.flytt(1)

        assertThat(res).isEqualTo(
            Periode(
                LocalDate.of(2024, NOVEMBER, 7),
                LocalDate.of(2024, NOVEMBER, 23),
            )
        )
    }
}