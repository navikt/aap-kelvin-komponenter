package no.nav.aap.komponenter.type

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

public class Periode(
    public val fom: LocalDate,
    public val tom: LocalDate
) : Comparable<Periode> {

    init {
        require(fom <= tom) { "tom($tom) er før fom($fom)" }
    }

    public fun overlapper(other: Periode): Boolean {
        return this.fom <= other.tom && other.fom <= this.tom
    }

    public fun inneholder(dato: LocalDate): Boolean {
        return dato in fom..tom
    }

    public fun antallDager(): Int {
        return fom.until(tom.plusDays(1), ChronoUnit.DAYS).toInt()
    }

    override fun compareTo(other: Periode): Int {
        val compareFom = fom.compareTo(other.fom)

        if (compareFom != 0) {
            return compareFom
        }

        return tom.compareTo(other.tom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Periode

        if (fom != other.fom) return false
        return tom == other.tom
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    override fun toString(): String {
        return "Periode(fom=$fom, tom=$tom)"
    }

    public fun jsonValue(): String {
        return "$fom/$tom"
    }

    public fun overlapp(other: Periode): Periode? {
        if (!this.overlapper(other)) {
            return null
        }
        if (this == other) {
            return this
        }
        return Periode(maxOf(fom, other.fom), minOf(tom, other.tom))
    }

    public fun minus(other: Periode): NavigableSet<Periode> {
        if (!this.overlapper(other)) {
            return TreeSet(listOf(this))
        }
        val resultat: NavigableSet<Periode> = TreeSet()
        if (fom < other.fom) {
            resultat.add(Periode(fom, minOf(tom, other.fom.minusDays(1))))
        }
        if (tom > other.tom) {
            resultat.add(Periode(maxOf(fom, other.tom.plusDays(1)), tom))
        }
        return resultat
    }

    public fun utvid(other: Periode): Periode {
        return Periode(minOf(this.fom, other.fom), maxOf(this.tom, other.tom))
    }

    public fun inntil(other: Periode): Boolean {
        return this.tom == other.fom.minusDays(1) || other.tom == this.fom.minusDays(1)
    }
}
