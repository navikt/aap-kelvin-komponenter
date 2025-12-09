package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate


public data class Segment<T>(public val periode: Periode, public val verdi: T) : Comparable<Segment<T>> {
    public fun overlapper(segment: Segment<*>): Boolean {
        return periode.overlapper(segment.periode)
    }

    internal fun forlengetKopi(periode: Periode): Segment<T> {
        val utvidetPeriode = this.periode.utvid(periode)
        return Segment(utvidetPeriode, verdi)
    }

    private fun inntil(other: Segment<T>): Boolean {
        return this.periode.inntil(other.periode)
    }

    public fun kanSammenslås(other: Segment<T>): Boolean {
        return inntil(other) && this.verdi == other.verdi
    }

    override fun compareTo(other: Segment<T>): Int {
        return this.periode.compareTo(other.periode)
    }

    public fun tilpassetPeriode(periode: Periode): Segment<T> {
        return Segment(periode, verdi)
    }

    public fun inneholder(dato: LocalDate): Boolean {
        return periode.inneholder(dato)
    }

    public fun fom(): LocalDate {
        return periode.fom
    }

    public fun tom(): LocalDate {
        return periode.tom
    }
}
