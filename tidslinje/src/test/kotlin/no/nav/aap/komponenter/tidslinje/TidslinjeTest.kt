package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.tidslinje.StandardSammenslåere.slåSammenTilListe
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeTest {

    @Test
    fun `all på tom tidslinje`() {
        val tomTidslinje = Tidslinje.empty<Unit>()
        val periode = Periode(LocalDate.now(), LocalDate.now())

        assertThat(tomTidslinje.all { false })
            .isTrue

        assertThat(tomTidslinje.all(sammenhengende = true) { false })
            .isTrue

        assertThat(tomTidslinje.all(iPeriode = periode) { false })
            .isTrue

        assertThat(tomTidslinje.all(iPeriode = periode, sammenhengende = true) { false })
            .isFalse
    }

    @Test
    fun `all på singelton-tidslinje`() {
        val now = LocalDate.now()
        val periode = Periode(now, now)
        val utenforPerioden = Periode(now + 1, now + 1)
        val tidslinje = tidslinjeOf(periode to 0)

        assertThat(tidslinje.all { it == 0 })
            .isTrue

        assertThat(tidslinje.all { it == 1 })
            .isFalse

        assertThat(tidslinje.all(sammenhengende = true) { it == 0 })
            .isTrue

        assertThat(tidslinje.all(sammenhengende = true) { it == 1 })
            .isFalse

        assertThat(tidslinje.all(iPeriode = periode) { it == 0 })
            .isTrue

        assertThat(tidslinje.all(iPeriode = utenforPerioden) { it == 0 })
            .isTrue

        assertThat(tidslinje.all(iPeriode = periode) { it == 1 })
            .isFalse

        assertThat(tidslinje.all(iPeriode = periode, sammenhengende = true) { it == 0 })
            .isTrue

        assertThat(tidslinje.all(iPeriode = utenforPerioden, sammenhengende = true) { it == 0 })
            .isFalse

        assertThat(tidslinje.all(iPeriode = periode, sammenhengende = true) { it == 1 })
            .isFalse

        assertThat(tidslinje.all(iPeriode = utenforPerioden, sammenhengende = true) { it == 1 })
            .isFalse
    }


    @Test
    fun `all med overlappende perioder`() {
        val now = LocalDate.now()
        val periodeA = Periode(now, now + 7)
        val periodeB = Periode(now + 8, now + 8)
        val periodeC = Periode(now + 10, now + 10)
        val periodeABC = Periode(now, now + 10)
        val periodeBC = Periode(now + 8, now + 10)
        val utenforPerioden = Periode(now + 11, now + 15)

        val tidslinje = tidslinjeOf(periodeA to 0, periodeB to 1, periodeC to 1)

        assertThat(tidslinje.all { it == 0 }).isFalse
        assertThat(tidslinje.all { it == 1 }).isFalse
        assertThat(tidslinje.all(sammenhengende = true) { it == 0 }).isFalse
        assertThat(tidslinje.all(sammenhengende = true) { it == 1 }).isFalse

        assertThat(tidslinje.all(iPeriode = periodeA) { it == 0 }).isTrue
        assertThat(tidslinje.all(iPeriode = periodeB) { it == 1 }).isTrue
        assertThat(tidslinje.all(iPeriode = periodeC) { it == 1 }).isTrue
        assertThat(tidslinje.all(iPeriode = periodeBC) { it == 1 }).isTrue
        assertThat(tidslinje.all(iPeriode = periodeBC, sammenhengende = true) { it == 1 }).isFalse

        assertThat(tidslinje.all(iPeriode = periodeABC) { it == 0 }).isFalse
        assertThat(tidslinje.all(iPeriode = periodeABC) { it == 1 }).isFalse
        assertThat(tidslinje.all(iPeriode = utenforPerioden) { it == 0 }).isTrue
        assertThat(tidslinje.all(iPeriode = utenforPerioden) { it == 1 }).isTrue

        assertThat(tidslinje.all(iPeriode = Periode(now + 1, now + 2), sammenhengende = true) { it == 0 })
            .isTrue
        assertThat(tidslinje.all(iPeriode = Periode(now + 1, now + 2), sammenhengende = true) { it == 0 })
            .isTrue
    }

    @Test
    fun `any med tomTidslinje`() {
        val tomTidslinje = Tidslinje.empty<Unit>()
        val now = LocalDate.now()
        val periode = Periode(now + 1, now + 1)

        assertThat(tomTidslinje.any { true }).isFalse
        assertThat(tomTidslinje.any { false }).isFalse
        assertThat(tomTidslinje.any(iPeriode = periode) { true }).isFalse
        assertThat(tomTidslinje.any(iPeriode = periode) { false }).isFalse
    }

    @Test
    fun `any med ikke-tom tidslinje`() {
        val now = LocalDate.now()
        val periodeA = Periode(now, now + 4)
        val periodeB = Periode(now + 5, now + 10)
        val periodeAB = Periode(now, now + 10)
        val overlappende = Periode(now + 5, now + 15)
        val utenfor = Periode(now + 11, now + 15)

        val tidslinje = tidslinjeOf(periodeA to 0, periodeB to 1)

        assertThat(tidslinje.any { it == 0 }).isTrue
        assertThat(tidslinje.any { it == 1 }).isTrue

        assertThat(tidslinje.any(iPeriode = periodeA) { it == 0 }).isTrue
        assertThat(tidslinje.any(iPeriode = periodeB) { it == 1 }).isTrue
        assertThat(tidslinje.any(iPeriode = periodeB) { it == 0 }).isFalse
        assertThat(tidslinje.any(iPeriode = periodeA) { it == 1 }).isFalse
        assertThat(tidslinje.any(iPeriode = utenfor) { false }).isFalse
        assertThat(tidslinje.any(iPeriode = periodeAB) { it == 0 }).isTrue
        assertThat(tidslinje.any(iPeriode = periodeAB) { it == 1 }).isTrue
        assertThat(tidslinje.any(iPeriode = overlappende) { it == 1 }).isTrue

    }

    @Test
    fun `Enkle tidslinjetester`() {
        val now = LocalDate.now()
        val periode1 = Periode(now, now.plusDays(4))
        val periode2 = Periode(now.plusDays(5), now.plusDays(10))
        val periode3 = Periode(now.plusDays(11), now.plusDays(15))

        val t1 = Tidslinje(Periode(periode1.fom, periode2.tom), 1)
        val t2 = Tidslinje(Periode(periode2.fom, periode3.tom), 2)

        assertThat(t1.outerJoin(t2) { v1, v2 -> Pair(v1, v2) })
            .isEqualTo(
                tidslinjeOf(
                    periode1 to Pair(1, null),
                    periode2 to Pair(1, 2),
                    periode3 to Pair(null, 2),
                )
            )

        assertThat(t1.leftJoin(t2) { v1, v2 -> Pair(v1, v2) })
            .isEqualTo(
                tidslinjeOf(
                    periode1 to Pair(1, null),
                    periode2 to Pair(1, 2),
                )
            )

        assertThat(t1.rightJoin(t2) { v1, v2 -> Pair(v1, v2) })
            .isEqualTo(
                tidslinjeOf(
                    periode2 to Pair(1, 2),
                    periode3 to Pair(null, 2),
                )
            )
    }

    @Test
    fun `skal lage tidslinje med verdier`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen perioder med lik verdi ved compress`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(100))
        val tidslinje = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
        assertThat(tidslinje.komprimer().segmenter()).containsExactly(
            Segment(
                Periode(
                    secondSegment.periode.fom,
                    firstSegment.periode.tom
                ), Beløp(100)
            )
        )
        // Bare så det er tydelig at compress ikke gjør inline manipulasjon
        assertThat(tidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.kombiner(
            tidslinje1,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        )

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, firstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val expectedFirstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje = tidslinje.kombiner(
            tidslinje1,
            StandardSammenslåere.prioriterHøyreSideCrossJoin()
        ).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(secondSegment, expectedFirstSegment)
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp med custom sammenslåer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje: Tidslinje<Beløp> =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.summerer()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)), Beløp(200)),
            Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1)), Beløp(300)),
            Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        )
    }

    @Test
    fun `skal kunne styre prioritet mellom tidslinjer`() {
        val firstSegment = Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val expectedSecondSegment =
            Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(3)), Beløp(200))
        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(listOf(secondSegment))

        val mergetTidslinje =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.prioriterVenstreSideCrossJoin())
                .komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(expectedSecondSegment, firstSegment)
    }

    @Test
    fun `slå sammen ulike typer`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(6))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(
            listOf(
                Segment(delPeriode1, Prosent(10)),
                Segment(delPeriode2, Prosent(50)),
                Segment(delPeriode3, Prosent(78))
            )
        )

        val mergetTidslinje = tidslinje.kombiner(tidslinje1, UtregningSammenslåer()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(delPeriode1, Utbetaling(beløp, Prosent(10))),
            Segment(delPeriode2, Utbetaling(beløp, Prosent(50))),
            Segment(delPeriode3, Utbetaling(beløp, Prosent(78)))
        )
    }

    @Test
    fun `skal slå sammen to tidslinjer med overlapp, frittstående elementer i en av tidslinjene og hull`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))
        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(7))
        val delPeriode2 = Periode(LocalDate.now().minusDays(5), LocalDate.now())
        val delPeriode3 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))
        val delPeriode4 = Periode(LocalDate.now().plusDays(15), LocalDate.now().plusDays(20))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val tidslinje = Tidslinje(listOf(firstSegment))
        val tidslinje1 = Tidslinje(
            listOf(
                Segment(delPeriode1, Beløp(10)),
                Segment(delPeriode2, Beløp(50)),
                Segment(delPeriode3, Beløp(78)),
                Segment(delPeriode4, Beløp(99))
            )
        )

        val mergetTidslinje: Tidslinje<Beløp> =
            tidslinje.kombiner(tidslinje1, StandardSammenslåere.prioriterHøyreSideCrossJoin()).komprimer()

        assertThat(mergetTidslinje.segmenter()).containsExactly(
            Segment(delPeriode1, Beløp(10)),
            Segment(Periode(LocalDate.now().minusDays(6), LocalDate.now().minusDays(6)), beløp),
            Segment(delPeriode2, Beløp(50)),
            Segment(delPeriode3, Beløp(78)),
            Segment(delPeriode4, Beløp(99))
        )
    }

    @Test
    fun `enkel test med barnetillegg`() {
        val fullPeriode = Periode(LocalDate.now().minusDays(10), LocalDate.now().plusDays(10))

        val delPeriode1 = Periode(LocalDate.now().minusDays(10), LocalDate.now())
        val delPeriode2 = Periode(LocalDate.now().plusDays(1), LocalDate.now().plusDays(10))

        val beløp = Beløp(756)
        val firstSegment = Segment(fullPeriode, beløp)

        val grunnlagTidslinje = Tidslinje(listOf(firstSegment))
        val barnetileggSats = Tidslinje(listOf(Segment(fullPeriode, Beløp(36))))
        val antallBarnTidslinje = Tidslinje(
            listOf(
                Segment(Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(10)), 1)
            )
        )
        val uttakTidslinje = Tidslinje(
            listOf(
                Segment(delPeriode1, Prosent(67)),
                Segment(delPeriode2, Prosent(73)),
            )
        )

        val barneUtreningTidslinje = antallBarnTidslinje.kombiner(barnetileggSats, BarneTileggUtbetaling())
        val komplettTidslinje =
            grunnlagTidslinje.kombiner(uttakTidslinje, UtregningSammenslåer())
                .kombiner(barneUtreningTidslinje, KombinertUtbetaling())

        assertThat(komplettTidslinje.segmenter()).hasSize(3)
    }

    @Test
    fun `begrens vha disjoint`() {
        val firstSegment = Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(10)), Beløp(100))
        val secondSegment = Segment(Periode(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1)), Beløp(200))
        val tidslinje = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        val res = tidslinje.begrensetTil(Periode(LocalDate.now().minusDays(5), LocalDate.now().plusDays(5)))
        assertThat(res.segmenter()).containsExactly(
            Segment(
                Periode(LocalDate.now().minusDays(5), LocalDate.now().minusDays(1)),
                Beløp(200)
            ), Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(5)), Beløp(100))

        )
    }


    @Test
    fun `kun høyre`() {
        val firstSegment = Segment(Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 3)), 1)
        val secondSegment = Segment(Periode(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 6)), 2)

        val venstre = Tidslinje(
            listOf(
                firstSegment,
                secondSegment
            )
        )

        val høyreSegment = Segment(Periode(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 1, 4)), 3)
        val høyre = Tidslinje(listOf(høyreSegment))

        val res = venstre.kombiner(høyre, StandardSammenslåere.kunHøyre())

        assertThat(res.segmenter()).containsExactly(
            Segment(
                Periode(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 1, 3)), 3
            )

        )
    }

    @Test
    fun `slå sammen til liste`() {
        val firstSegmentA = Segment(Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 3)), listOf(1))
        val andreSegmentA = Segment(Periode(LocalDate.of(2020, 1, 4), LocalDate.of(2020, 1, 10)), listOf(2))

        val firstSegmentB = Segment(Periode(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 1, 2)), 3)
        val andreSegmentB = Segment(Periode(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 7)), 4)

        val tidslinje1 = Tidslinje(listOf(firstSegmentA, andreSegmentA))
        val tidslinje2 = Tidslinje(listOf(firstSegmentB, andreSegmentB))

        val res = tidslinje1.kombiner(tidslinje2, slåSammenTilListe())

        printBinaryFunction(tidslinje1, tidslinje2, slåSammenTilListe())

        assertThat(res.segmenter()).containsExactly(
            Segment(Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1)), listOf(1)),
            Segment(Periode(LocalDate.of(2020, 1, 2), LocalDate.of(2020, 1, 2)), listOf(1, 3)),
            Segment(Periode(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 3)), listOf(1, 4)),
            Segment(Periode(LocalDate.of(2020, 1, 4), LocalDate.of(2020, 1, 7)), listOf(2, 4)),
            Segment(Periode(LocalDate.of(2020, 1, 8), LocalDate.of(2020, 1, 10)), listOf(2))
        )
    }

    @Test
    fun `skal hente ut verdier fra tidslinje`() {
        val tidslinje = Tidslinje(
            listOf(
                Segment(Periode(LocalDate.now(), LocalDate.now().plusDays(5)), Beløp(100)),
                Segment(Periode(LocalDate.now().plusDays(6), LocalDate.now().plusDays(10)), Beløp(200))
            )
        )

        assertThat(tidslinje.verdier()).containsExactly(Beløp(100), Beløp(200))
    }

    @Test
    fun `slett blir ikke forvirret av null-verdier`() {
        assertThat(
            tidslinjeOf(
                Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-30")) to null
            ).slett(Periode(LocalDate.parse("2020-01-04"), LocalDate.parse("2020-02-01")))
        ).isEqualTo(
            tidslinjeOf(
                Periode(LocalDate.parse("2020-01-01"), LocalDate.parse("2020-01-03")) to null,
            )
        )
    }

    @Test
    fun `map7 uten periode - alle tidslinjer overlapper`() {
        val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10))
        val a = Tidslinje(periode, 1)
        val b = Tidslinje(periode, 2)
        val c = Tidslinje(periode, 3)
        val d = Tidslinje(periode, 4)
        val e = Tidslinje(periode, 5)
        val f = Tidslinje(periode, 6)
        val g = Tidslinje(periode, 7)

        val resultat = Tidslinje.map7(a, b, c, d, e, f, g) { av, bv, cv, dv, ev, fv, gv ->
            (av ?: 0) + (bv ?: 0) + (cv ?: 0) + (dv ?: 0) + (ev ?: 0) + (fv ?: 0) + (gv ?: 0)
        }

        assertThat(resultat).isEqualTo(tidslinjeOf(periode to 28))
    }

    @Test
    fun `map7 med delvis overlapp`() {
        val periodeABC = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10))
        val periodeDEFG = Periode(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 15))

        val a = Tidslinje(periodeABC, "a")
        val b = Tidslinje(periodeABC, "b")
        val c = Tidslinje(periodeABC, "c")
        val d = Tidslinje(periodeDEFG, "d")
        val e = Tidslinje(periodeDEFG, "e")
        val f = Tidslinje(periodeDEFG, "f")
        val g = Tidslinje(periodeDEFG, "g")

        val resultat = Tidslinje.map7(a, b, c, d, e, f, g) { av, bv, cv, dv, ev, fv, gv ->
            listOfNotNull(av, bv, cv, dv, ev, fv, gv).joinToString("")
        }

        assertThat(resultat).isEqualTo(
            tidslinjeOf(
                Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 4)) to "abc",
                Periode(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 10)) to "abcdefg",
                Periode(LocalDate.of(2020, 1, 11), LocalDate.of(2020, 1, 15)) to "defg",
            )
        )
    }

    @Test
    fun `ifEmpty returnerer defaultValue når tidslinja er tom`() {
        val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10))
        val default = Tidslinje(periode, 42)

        val resultat = Tidslinje.empty<Int>().ifEmpty { default }

        assertThat(resultat).isEqualTo(default)
    }

    @Test
    fun `ifEmpty returnerer original tidslinje når den ikke er tom`() {
        val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10))
        val original = Tidslinje(periode, 1)
        val default = Tidslinje(periode, 42)

        val resultat = original.ifEmpty { default }

        assertThat(resultat).isEqualTo(original)
    }

    @Test
    fun `ifEmpty støtter early return når tidslinja er tom`() {
        fun behandle(tidslinje: Tidslinje<Int>): String {
            tidslinje.ifEmpty { return "tom" }
            return "ikke tom"
        }

        assertThat(behandle(Tidslinje.empty())).isEqualTo("tom")
        assertThat(
            behandle(
                Tidslinje(
                    Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10)),
                    1
                )
            )
        ).isEqualTo("ikke tom")
    }

    @Test
    fun `ifNotEmpty støtter early return når tidslinja ikke er tom`() {
        fun behandle(tidslinje: Tidslinje<Int>): String {
            tidslinje.ifNotEmpty { return "ikke tom" }
            return "tom"
        }

        assertThat(behandle(Tidslinje.empty())).isEqualTo("tom")
        assertThat(
            behandle(
                Tidslinje(
                    Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10)),
                    1
                )
            )
        ).isEqualTo("ikke tom")
    }

    @Test
    fun `map7 med tom tidslinje`() {
        val periode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 10))
        val a = Tidslinje(periode, 1)
        val b = Tidslinje(periode, 2)
        val c = Tidslinje(periode, 3)
        val d = Tidslinje(periode, 4)
        val e = Tidslinje(periode, 5)
        val f = Tidslinje(periode, 6)
        val g = Tidslinje.empty<Int>()

        val resultat = Tidslinje.map7(a, b, c, d, e, f, g) { av, bv, cv, dv, ev, fv, gv ->
            (av ?: 0) + (bv ?: 0) + (cv ?: 0) + (dv ?: 0) + (ev ?: 0) + (fv ?: 0) + (gv ?: 0)
        }

        assertThat(resultat).isEqualTo(tidslinjeOf(periode to 21))
    }
}

data class Utbetaling(val beløp: Beløp, val prosent: Prosent) {
    fun beløp(): Beløp {
        return prosent.multiplisert(beløp)
    }

    override fun toString(): String {
        return "Utbetaling(beløp=$beløp, prosent=$prosent, utbetaling=${beløp()})"
    }
}

data class UtbetalingMedBarneTilegg(val beløp: Beløp, val barnetilegg: Beløp, val prosent: Prosent) {
    fun beløp(): Beløp {
        return prosent.multiplisert(beløp.pluss(barnetilegg))
    }

    override fun toString(): String {
        return "Utbetaling(beløp=$beløp, barnetilegg=$barnetilegg, prosent=$prosent, utbetaling=${beløp()})"
    }
}

class BarneTileggUtbetaling :
    JoinStyle<Int, Beløp, Beløp> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            val prosent = venstreSegment?.verdi ?: 0
            val beløp = høyreSegment?.verdi ?: Beløp(0)
            Segment(periode, beløp.multiplisert(prosent))
        })

class KombinertUtbetaling :
    JoinStyle<Utbetaling, Beløp, UtbetalingMedBarneTilegg> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            if (venstreSegment == null) {
                return@OUTER_JOIN null
            }
            val beløp = høyreSegment?.verdi ?: Beløp(0)
            Segment(periode, UtbetalingMedBarneTilegg(venstreSegment.verdi.beløp, beløp, venstreSegment.verdi.prosent))
        })

class UtregningSammenslåer :
    JoinStyle<Beløp, Prosent, Utbetaling> by JoinStyle.OUTER_JOIN(
        { periode: Periode, venstreSegment, høyreSegment ->
            val beløp = venstreSegment?.verdi ?: Beløp(0)
            val prosent = høyreSegment?.verdi ?: Prosent(0)
            Segment(periode, Utbetaling(beløp, prosent))
        }
    )

private operator fun LocalDate.plus(days: Int): LocalDate = this.plusDays(days.toLong())
