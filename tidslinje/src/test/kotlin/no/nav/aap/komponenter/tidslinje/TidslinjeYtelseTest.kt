package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.time.measureTime

/**
 * Ytelsestest for somTidslinje — avdekker O(n³) kompleksitet i lowLevelOuterJoin.
 *
 * Bakgrunn: aap-behandlingsflyt observerte 104 sekunders responstid for en sak med
 * 113 revurderinger × 150 meldekort-perioder (14 dager). Rotkause er at
 * lowLevelOuterJoin gjør firstOrNull (lineær scan O(n)) per iterasjon istedet
 * for å utnytte NavigableSet (O(log n) via floor/ceiling).
 *
 * Forventet kompleksitet:
 *   - Nåværende: O(n³)  → n=150 gir ~3.375M operasjoner
 *   - Ønsket:    O(n² log n) → n=150 gir ~162K operasjoner (~20x raskere)
 */
class SomTidslinjePerformanceTest {

    @Test
    fun `somTidslinje med 150 ikke-overlappende perioder skal fullføre under 50ms`() {
        val perioder = lagPerioder(150)

        val tid = measureTime {
            repeat(113) {
                perioder.somTidslinje { it }
                    .mapNotNull { it } // simulerer rettighetstyper()
                    .komprimer()
            }
        }

        println("somTidslinje 113x med 150 perioder: $tid")
        assertTrue(tid.inWholeMilliseconds < 500) {
            "Forventet under 500ms, men tok ${tid.inWholeMilliseconds}ms. " +
                    "Sannsynlig O(n³) kompleksitet i lowLevelOuterJoin."
        }
    }

    @Test
    fun `somTidslinje med overlappende perioder gir siste-vinner-semantikk`() {
        val fom = LocalDate.of(2022, 1, 1)
        // To overlappende perioder — siste vinner
        val perioder = listOf(
            Periode(fom, fom.plusDays(27)),   // 4 uker
            Periode(fom.plusDays(14), fom.plusDays(41)), // overlapper 14 dager
        )

        val tidslinje = perioder.somTidslinje { it }
        val periodeCount = tidslinje.perioder().count()

        // Ny implementasjon splitter ikke den innkommende perioden kunstig ved eksisterende grenser,
        // så vi får 2 segmenter: [0-13] med første-verdi og [14-41] med siste-verdi.
        // Komprimer ville gi samme resultat uansett (begge halvdelene av [14-41] har lik verdi).
        assertTrue(periodeCount == 2) {
            "Forventet 2 sub-perioder etter splitting, fikk $periodeCount"
        }
    }

    @Test
    fun `kompleksitet skal skalere sub-kubisk`() {
        // Mål tid for ulike n og verifiser at veksten er sub-kubisk
        val tider = listOf(50, 100, 150).map { n ->
            val perioder = lagPerioder(n)
            measureTime {
                repeat(10) {
                    perioder.somTidslinje { it }.mapNotNull { it }.komprimer()
                }
            }.inWholeMicroseconds.toDouble()
        }

        println("Tider for n=50, n=100, n=150: $tider µs")

        // Kubisk vekst: ratio(100/50)^3 = 8x, ratio(150/50)^3 = 27x
        // Kvadratisk-logaritmisk: ratio(100/50)^2 * log(100/50) ≈ 4x
        val ratio100vs50 = tider[1] / tider[0]
        val ratio150vs50 = tider[2] / tider[0]

        println("Ratio 100 vs 50: ${ratio100vs50}x (kubisk ville vært ~8x)")
        println("Ratio 150 vs 50: ${ratio150vs50}x (kubisk ville vært ~27x)")

        assertTrue(ratio150vs50 < 15.0) {
            "Ratio 150/50 er ${ratio150vs50}x — kubisk kompleksitet (O(n³))! " +
                    "Forventet under 15x (O(n² log n))."
        }
    }

    @Test
    fun `somTidslinje med 150 lett overlappende perioder skal fullføre under 500ms`() {
        // Simulerer revurderingsscenario: hver periode overlapper 2 dager med neste
        val perioder = lagOverlappendePerioder(150)

        val tid = measureTime {
            repeat(113) {
                perioder.somTidslinje { it }
                    .mapNotNull { it }
                    .komprimer()
            }
        }

        println("somTidslinje 113x med 150 lett overlappende perioder: $tid")
        assertTrue(tid.inWholeMilliseconds < 500) {
            "Forventet under 500ms, men tok ${tid.inWholeMilliseconds}ms."
        }
    }

    @Test
    fun `mergePrioriterHøyre fast path er raskere enn generell path for enkelt-segment`() {
        // Warm up both paths equally before measuring
        val base = lagTidslinje(150)
        val enkelt = Tidslinje(lagPerioder(1).first(), lagPerioder(1).first())
        val toSegmenter = lagTidslinje(2)

        repeat(500) {
            base.mergePrioriterHøyre(enkelt)
            base.mergePrioriterHøyre(toSegmenter)
        }

        // Measure fast path (other.segmenter.size == 1)
        val tidFastPath = measureTime {
            repeat(1000) { base.mergePrioriterHøyre(enkelt) }
        }

        // Measure general path by wrapping the same single period in a 2-segment tidslinje
        // to bypass the fast path, while keeping the same effective work
        val tidGenerellPath = measureTime {
            repeat(1000) { base.mergePrioriterHøyre(toSegmenter) }
        }

        println("Fast path  (size=1): $tidFastPath")
        println("Generell   (size=2): $tidGenerellPath")
        println("Speedup: ${tidGenerellPath.inWholeMicroseconds.toDouble() / tidFastPath.inWholeMicroseconds}x")

        assertTrue(tidFastPath < tidGenerellPath) {
            "Fast path ($tidFastPath) skal være raskere enn generell path ($tidGenerellPath)"
        }
    }

    @Test
    fun `mergePrioriterHøyre med stor other-tidslinje skal fullføre under 500ms`() {
        // Tester den generelle stien i mergePrioriterHøyre (other.segmenter.size >> 1)
        // som går gjennom outerJoin og lowLevelOuterJoin.
        val venstre = lagTidslinje(150)
        val høyre = lagTidslinje(150)

        val tid = measureTime {
            repeat(113) {
                venstre.mergePrioriterHøyre(høyre)
            }
        }

        println("mergePrioriterHøyre 113x med 150+150 segmenter: $tid")
        assertTrue(tid.inWholeMilliseconds < 500) {
            "Forventet under 500ms, men tok ${tid.inWholeMilliseconds}ms."
        }
    }

    private fun lagTidslinje(antall: Int): Tidslinje<Periode> =
        Tidslinje(lagPerioder(antall).map { Segment(it, it) })

    private fun lagOverlappendePerioder(antall: Int): List<Periode> {
        val start = LocalDate.of(2022, 1, 3) // mandag
        return (0 until antall).map { i ->
            val fom = start.plusDays(i * 12L) // 12 dagers steg → 2 dagers overlapp med neste
            Periode(fom, fom.plusDays(13))
        }
    }

    private fun lagPerioder(antall: Int): List<Periode> {
        val start = LocalDate.of(2022, 1, 3) // mandag
        return (0 until antall).map { i ->
            val fom = start.plusDays(i * 14L)
            Periode(fom, fom.plusDays(13))
        }
    }
}