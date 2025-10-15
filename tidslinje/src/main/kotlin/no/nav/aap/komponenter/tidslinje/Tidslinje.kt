package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.Period
import java.util.*


public class Tidslinje<T>(initSegmenter: NavigableSet<Segment<T>> = TreeSet()) {


    public constructor(initSegmenter: List<Segment<T>>) : this(TreeSet(initSegmenter))
    public constructor(periode: Periode, verdi: T) : this(TreeSet(listOf(Segment(periode, verdi))))

    private val segmenter: NavigableSet<Segment<T>> = TreeSet(initSegmenter)

    init {
        // Sjekk etter overlapp
        validerIkkeOverlapp()
    }

    private fun validerIkkeOverlapp() {
        var last: Segment<T>? = null
        for (seg in segmenter) {
            if (last != null) {
                require(!seg.overlapper(last)) { String.format("Overlapp %s - %s", last, seg) }
            }
            last = seg
        }
    }

    public fun isEmpty(): Boolean {
        return segmenter.isEmpty()
    }

    public fun isNotEmpty(): Boolean {
        return segmenter.isNotEmpty()
    }

    public fun segmenter(): Iterable<Segment<T>> {
        return segmenter
    }

    public fun perioder(): Iterable<Periode> {
        return segmenter.mapTo(TreeSet(), Segment<T>::periode)
    }

    private fun <E, V> lowLevelOuterJoin(
        other: Tidslinje<E>,
        body: (Periode, Segment<T>?, Segment<E>?) -> Segment<V>?,
    ): Tidslinje<V> {
        if (this.segmenter.isEmpty()) {
            val nyeSegmenter = other.segmenter.mapNotNullTo(TreeSet()) { segment ->
                body(segment.periode, null, segment)
            }
            return Tidslinje(nyeSegmenter)
        }
        if (other.segmenter.isEmpty()) {
            val nyeSegmenter = this.segmenter.mapNotNullTo(TreeSet()) { segment ->
                body(segment.periode, segment, null)
            }
            return Tidslinje(nyeSegmenter)
        }

        val periodeIterator = PeriodeIterator(
            perioder(),
            other.perioder()
        )

        val nySammensetning: NavigableSet<Segment<V>> = TreeSet()
        while (periodeIterator.hasNext()) {
            val periode = periodeIterator.next()

            val left = this.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }
                ?.tilpassetPeriode(periode)
            val right = other.segmenter.firstOrNull { segment -> segment.periode.overlapper(periode) }
                ?.tilpassetPeriode(periode)

            val kombinert = body(periode, left, right)
            if (kombinert != null) {
                nySammensetning.add(kombinert)
            }
        }

        return Tidslinje(nySammensetning)
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    public fun <E, V> kombiner(
        other: Tidslinje<E>,
        joinStyle: JoinStyle<T, E, V>
    ): Tidslinje<V> {
        return lowLevelOuterJoin(other, joinStyle::kombiner)
    }

    /**
     * Begrens tidslinjen til [periode].
     */
    public fun begrensetTil(periode: Periode): Tidslinje<T> {
        return kombiner(
            Tidslinje(periode, null),
            StandardSammenslåere.kunVenstre()
        )
    }

    @Deprecated("Benytt begrensTil", ReplaceWith("begrensetTil"))
    public fun disjoint(periode: Periode): Tidslinje<T> {
        return kombiner(
            Tidslinje(periode, null),
            StandardSammenslåere.kunVenstre()
        )
    }

    @Deprecated("Benytt begrensTil", ReplaceWith("begrensetTil"))
    public fun kryss(periode: Periode): Tidslinje<T> {
        return kombiner(
            Tidslinje(periode, null),
            StandardSammenslåere.kunVenstre()
        )
    }

    public fun <E, V> disjoint(
        other: Tidslinje<E>,
        combinator: (Periode, Segment<T>) -> Segment<V>
    ): Tidslinje<V> {
        return kombiner(other, JoinStyle.DISJOINT(combinator))
    }


    public fun kryss(other: Tidslinje<Any?>): Tidslinje<T> {
        return kombiner(other, StandardSammenslåere.kunVenstre())
    }

    public fun filter(predikat: (Segment<T>) -> Boolean): Tidslinje<T> {
        if (isEmpty()) return this
        return Tidslinje(segmenter.filter(predikat))
    }

    /**
     * Komprimerer tidslinjen
     * - Slår sammen segmenter hvor verdien er identisk (benytter equals for sjekk)
     */
    public fun komprimer(): Tidslinje<T> {
        val compressedSegmenter: List<Segment<T>> = segmenter.fold(emptyList()) { acc, neste ->
            if (acc.isEmpty()) {
                return@fold listOf(neste)
            }

            val siste = acc.last()

            if (siste.kanSammenslås(neste)) {
                return@fold acc.dropLast(1) + siste.forlengetKopi(neste.periode)
            }

            acc + neste
        }
        return Tidslinje(compressedSegmenter)
    }

    public fun <R> map(mapper: (T) -> R): Tidslinje<R> {
        return map { _, verdi -> mapper(verdi) }
    }

    public fun <R> map(mapper: (Periode, T) -> R): Tidslinje<R> {
        return Tidslinje(segmenter.mapTo(TreeSet()) { s ->
            Segment(s.periode, mapper(s.periode, s.verdi))
        })
    }

    public fun <R> mapNotNull(mapper: (T) -> R?): Tidslinje<R> {
        return mapNotNull { _, verdi -> mapper(verdi) }
    }

    public fun <R> mapNotNull(mapper: (Periode, T) -> R?): Tidslinje<R> {
        return Tidslinje(segmenter.mapNotNullTo(TreeSet()) { s ->
            mapper(s.periode, s.verdi)?.let { Segment(s.periode, it) }
        })
    }


    public fun <R> mapValue(mapper: (T) -> R): Tidslinje<R> {
        return map(mapper)
    }

    public fun splittOppEtter(period: Period): Tidslinje<T> {
        if (segmenter.isEmpty()) {
            return this
        }
        return splittOppEtter(minDato(), maxDato(), period)
    }

    public fun splittOppEtter(startDato: LocalDate, period: Period): Tidslinje<T> {
        if (segmenter.isEmpty()) {
            return this
        }
        return splittOppEtter(startDato, maxDato(), period)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    public fun splittOppEtter(startDato: LocalDate, sluttDato: LocalDate, period: Period): Tidslinje<T> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<T>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = begrensetTil(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(nesteSegmenter)
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    public fun <R> splittOppOgMapOmEtter(
        period: Period,
        mapper: (NavigableSet<Segment<T>>) -> NavigableSet<Segment<R>>
    ): Tidslinje<R> {
        if (segmenter.isEmpty()) {
            return Tidslinje()
        }
        return splittOppOgMapOmEtter(minDato(), maxDato(), period, mapper)
    }

    /**
     * Knekker opp segmenterene i henhold til period fom startDato tom sluttDato
     */
    public fun <R> splittOppOgMapOmEtter(
        startDato: LocalDate,
        sluttDato: LocalDate,
        period: Period,
        mapper: (NavigableSet<Segment<T>>) -> NavigableSet<Segment<R>>
    ): Tidslinje<R> {
        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val segmenter: NavigableSet<Segment<R>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)

            val nesteSegmenter: NavigableSet<Segment<T>> = begrensetTil(Periode(dt, nextDt.minusDays(1))).segmenter
            segmenter.addAll(mapper(nesteSegmenter))
            dt = nextDt
        }
        return Tidslinje(segmenter)
    }

    /* Knekker opp segmenterene i henhold til period fom startDato tom sluttDato, og grupperer
     * alle segmentene innenfor periodene som tidslinjer. */
    public fun splittOppOgGrupper(
        startDato: LocalDate,
        sluttDato: LocalDate,
        period: Period,
    ): Tidslinje<Tidslinje<T>> {
        if (this.segmenter.isEmpty()) {
            return Tidslinje()
        }

        require(!(LocalDate.MIN == startDato || LocalDate.MAX == sluttDato || sluttDato.isBefore(startDato))) {
            String.format(
                "kan ikke periodisere tidslinjen mellom angitte datoer: [%s, %s]",
                startDato,
                sluttDato
            )
        }

        val tidslinjer: NavigableSet<Segment<Tidslinje<T>>> = TreeSet()

        val maxLocalDate: LocalDate = minOf(maxDato(), sluttDato)
        var dt = startDato
        while (!dt.isAfter(maxLocalDate)) {
            val nextDt = dt.plus(period)
            val p = Periode(dt, nextDt.minusDays(1))
            tidslinjer.add(Segment(p, begrensetTil(p)))
            dt = nextDt
        }

        return Tidslinje(tidslinjer)
    }

    public fun <R> flatMap(mapper: (Segment<T>) -> Tidslinje<R>): Tidslinje<R> {
        return Tidslinje(segmenter().flatMap {
            mapper(it).segmenter()
        })
    }

    public fun splittOppKalenderår(): Tidslinje<Tidslinje<T>> {
        if (segmenter.isEmpty()) return Tidslinje()
        val førsteDagFørsteKalenderår = segmenter.first.periode.fom.withDayOfYear(1)
        val sisteDag = segmenter.last.periode.tom
        val sisteDagSisteKalenderår = sisteDag.withDayOfYear(sisteDag.lengthOfYear())
        return splittOppOgGrupper(førsteDagFørsteKalenderår, sisteDagSisteKalenderår, Period.ofYears(1))
    }

    public fun splittOppIPerioder(
        perioder: List<Periode>,
    ): Tidslinje<Tidslinje<T>> {
        val perioderÅSplitteOppI: Tidslinje<Periode> = perioder
            .map { periode -> Segment(periode, periode) }
            .let { Tidslinje(it) }

        val verdierMedPeriodeTidslinje: Tidslinje<Segment<T>> =
            perioderÅSplitteOppI.kombiner(this, JoinStyle.RIGHT_JOIN { periode, splittPeriode, tSegment ->
                if (splittPeriode == null) {
                    null
                } else {
                    Segment(periode, Segment(splittPeriode.verdi, tSegment.verdi))
                }
            })

        return verdierMedPeriodeTidslinje
            .segmenter()
            .groupBy(
                { segment -> segment.verdi.periode },
                { segment -> Segment(segment.periode, segment.verdi.verdi) }
            )
            .map { (splittPeriode, segmenter) ->
                Segment(splittPeriode, Tidslinje(segmenter))
            }
            .let { Tidslinje(it) }
    }

    /**
     * Henter segmentet som inneholder datoen
     */
    public fun segment(dato: LocalDate): Segment<T>? {
        return segmenter.firstOrNull { segment -> segment.inneholder(dato) }
    }

    public fun minDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.first().fom()
    }

    public fun maxDato(): LocalDate {
        check(!segmenter.isEmpty()) {
            "Timeline is empty" //$NON-NLS-1$
        }
        return segmenter.last().tom()
    }

    public fun helePerioden(): Periode {
        return Periode(minDato(), maxDato())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tidslinje<*>

        // Benytter hashset for å slippe rør med compareTo osv..
        return HashSet(segmenter) == HashSet(other.segmenter)
    }

    override fun hashCode(): Int {
        return segmenter.hashCode()
    }

    override fun toString(): String {
        return "Tidslinje(segmenter=$segmenter)"
    }

    public fun erSammenhengende(): Boolean {
        return segmenter.windowed(2).all {
            it[0].tom().plusDays(1) == it[1].fom()
        }
    }

    public fun <U, R> outerJoin(other: Tidslinje<U>, body: (T?, U?) -> R): Tidslinje<R> {
        return outerJoin(other) { _, t, u -> body(t, u) }
    }

    public fun <U, R> outerJoin(other: Tidslinje<U>, body: (Periode, T?, U?) -> R): Tidslinje<R> {
        return lowLevelOuterJoin(other) { periode, thisSegment, otherSegment ->
            Segment(periode, body(periode, thisSegment?.verdi, otherSegment?.verdi))
        }
    }

    public fun <U, R> outerJoinNotNull(other: Tidslinje<U>, body: (Periode, T?, U?) -> R?): Tidslinje<R> {
        return lowLevelOuterJoin(other) { periode, thisSegment, otherSegment ->
            val verdi = body(periode, thisSegment?.verdi, otherSegment?.verdi)
            if (verdi == null) null else Segment(periode, verdi)
        }
    }

    public fun <U, R> outerJoinNotNull(other: Tidslinje<U>, body: (T?, U?) -> R?): Tidslinje<R> {
        return outerJoinNotNull(other) { _, left, right -> body(left, right) }
    }

    public fun <U, R> innerJoin(other: Tidslinje<U>, body: (Periode, T, U) -> R): Tidslinje<R> {
        return lowLevelOuterJoin(other) { periode, thisSegment, otherSegment ->
            if (thisSegment == null || otherSegment == null) {
                null
            } else {
                Segment(periode, body(periode, thisSegment.verdi, otherSegment.verdi))
            }
        }
    }

    public fun <U, R> innerJoin(other: Tidslinje<U>, body: (T, U) -> R): Tidslinje<R> {
        return innerJoin(other) { _, left, right -> body(left, right) }
    }

    public fun <U, R> leftJoin(other: Tidslinje<U>, body: (Periode, T, U?) -> R): Tidslinje<R> {
        return lowLevelOuterJoin(other) { periode, leftSegment, rightSegment ->
            if (leftSegment == null) {
                null
            } else {
                Segment(periode, body(periode, leftSegment.verdi, rightSegment?.verdi))
            }
        }
    }

    public fun <U, R> leftJoin(other: Tidslinje<U>, body: (T, U?) -> R): Tidslinje<R> {
        return leftJoin(other) { _, left, right -> body(left, right) }
    }

    public fun <U, R> rightJoin(other: Tidslinje<U>, body: (Periode, T?, U) -> R): Tidslinje<R> {
        return other.leftJoin(this) { periode, right, left ->
            body(periode, left, right)
        }
    }

    public fun <U, R> rightJoin(other: Tidslinje<U>, body: (T?, U) -> R): Tidslinje<R> {
        return rightJoin(other) { _, left, right -> body(left, right) }
    }

    /** Lag tidslinje for de periodene som ikke er i [this]. */
    public fun <U> komplement(periode: Periode, body: (Periode) -> U): Tidslinje<U> {
        return outerJoinNotNull(Tidslinje(periode, Unit)) { segmentPeriode, eksisterende, _ ->
            if (eksisterende == null) {
                body(segmentPeriode)
            } else {
                null
            }
        }
    }

    public fun <Resultat> fold(init: Resultat, f: (Resultat, Periode, T) -> Resultat): Resultat {
        return segmenter.fold(init) { acc, segment -> f(acc, segment.periode, segment.verdi) }
    }

    public fun <Resultat> fold(init: Resultat, f: (Resultat, T) -> Resultat): Resultat {
        return segmenter.fold(init) { acc, segment -> f(acc, segment.verdi) }
    }

    public fun mergePrioriterHøyre(other: Tidslinje<T>): Tidslinje<T> {
        return this.outerJoin(other) { venstreVerdi, høyreVerdi -> høyreVerdi ?: venstreVerdi ?: error("ikke mulig") }
    }

    public companion object {
        public fun <T> empty(): Tidslinje<T> = Tidslinje<T>(TreeSet())

        public fun <A, B> zip2(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
        ): Tidslinje<Pair<A?, B?>> {
            return aTidslinje.outerJoin(bTidslinje) { a, b -> Pair(a, b) }
        }

        public fun <A, B, C> zip3(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>
        ): Tidslinje<Triple<A?, B?, C?>> {
            return zip2(aTidslinje, bTidslinje)
                .outerJoin(cTidslinje) { ab, c ->
                    Triple(ab?.first, ab?.second, c)
                }
        }

        public fun <A, B, R> map2(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            body: (A?, B?) -> R,
        ): Tidslinje<R> {
            return zip2(aTidslinje, bTidslinje).map { (a, b) -> body(a, b) }
        }

        public fun <A, B, C, R> map3(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>,
            body: (A?, B?, C?) -> R,
        ): Tidslinje<R> {
            return zip3(aTidslinje, bTidslinje, cTidslinje).map { (a, b, c) -> body(a, b, c) }
        }

        public fun <A, B, C, D, R> map4(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>,
            dTidslinje: Tidslinje<D>,
            body: (A?, B?, C?, D?) -> R,
        ): Tidslinje<R> {
            return zip3(aTidslinje, bTidslinje, cTidslinje).outerJoin(dTidslinje) { abc, d ->
                body(abc?.first, abc?.second, abc?.third, d)
            }
        }

        public fun <A, B, C, D, E, R> map5(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>,
            dTidslinje: Tidslinje<D>,
            eTidslinje: Tidslinje<E>,
            body: (Periode, A?, B?, C?, D?, E?) -> R,
        ): Tidslinje<R> {
            return zip3(aTidslinje, bTidslinje, cTidslinje)
                .outerJoin(zip2(dTidslinje, eTidslinje)) { periode, abc, de ->
                    body(periode, abc?.first, abc?.second, abc?.third, de?.first, de?.second)
                }
        }

        public fun <A, B, C, D, E, R> map5(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>,
            dTidslinje: Tidslinje<D>,
            eTidslinje: Tidslinje<E>,
            body: (A?, B?, C?, D?, E?) -> R,
        ): Tidslinje<R> {
            return zip3(aTidslinje, bTidslinje, cTidslinje)
                .outerJoin(zip2(dTidslinje, eTidslinje)) { abc, de ->
                    body(abc?.first, abc?.second, abc?.third, de?.first, de?.second)
                }
        }

        public fun <A, B, C, D, E, F, R> map6(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>,
            dTidslinje: Tidslinje<D>,
            eTidslinje: Tidslinje<E>,
            fTidslinje: Tidslinje<F>,
            body: (A?, B?, C?, D?, E?, F?) -> R,
        ): Tidslinje<R> {
            return zip3(aTidslinje, bTidslinje, cTidslinje)
                .outerJoin(zip3(dTidslinje, eTidslinje, fTidslinje)) { abc, def ->
                    body(abc?.first, abc?.second, abc?.third, def?.first, def?.second, def?.third)
                }
        }
}
}

public fun <T> tidslinjeOf(vararg segments: Pair<Periode, T>): Tidslinje<T> {
    return Tidslinje(segments.map { Segment(it.first, it.second) })
}

public fun <T> Tidslinje<T?>.filterNotNull(): Tidslinje<T> {
    return this.mapNotNull { it }
}

public fun <T> Iterable<Tidslinje<T>>.outerJoinKeepNulls(): Tidslinje<List<T?>> {
    return this.fold(Tidslinje()) { listeTidslinje, elementTidslinje ->
        listeTidslinje.outerJoin(elementTidslinje) { liste, element ->
            liste.orEmpty() + listOf(element)
        }
    }
}

public fun <T, S> Iterable<Tidslinje<T>>.outerJoinKeepNulls(action: (List<T?>) -> S): Tidslinje<S> {
    return this.outerJoinKeepNulls().mapValue(action)
}

public fun <T, S> Iterable<Tidslinje<T>>.outerJoinKeepNullsNotNull(action: (List<T?>) -> S?): Tidslinje<S> {
    return this.outerJoinKeepNulls().mapNotNull(action)
}

public fun <T> Iterable<Tidslinje<T>>.outerJoin(): Tidslinje<List<T>> {
    return this.fold(Tidslinje()) { listeTidslinje, elementTidslinje ->
        listeTidslinje.outerJoin(elementTidslinje) { liste, element ->
            liste.orEmpty() + listOfNotNull(element)
        }
    }
}

public fun <T, S> Iterable<Tidslinje<T>>.outerJoin(action: (List<T>) -> S): Tidslinje<S> {
    return this.outerJoin().mapValue(action)
}

public fun <T, S> Iterable<Tidslinje<T>>.outerJoinNotNull(action: (List<T>) -> S?): Tidslinje<S> {
    return this.outerJoin(action).filterNotNull()
}

public fun <T> Tidslinje<T>?.orEmpty(): Tidslinje<T> = this ?: Tidslinje.empty()

/** Lag tidslinje basert på verdiene ved å knytte dem til perioder. Verdier lenger ut
 * i lista får høyere prioritet, og legger seg over tidligere verdier. */
public fun <T, R> Iterable<T>.somTidslinje(periodeSelector: (T) -> Periode, valueSelector: (T) -> R): Tidslinje<R> {
    return fold(Tidslinje<R>()) { tidslinje, neste ->
        tidslinje.mergePrioriterHøyre(Tidslinje(periodeSelector(neste), valueSelector(neste)))
    }
}

/** Lag tidslinje basert på verdiene ved å knytte dem til perioder. Verdier lenger ut
 * i lista får høyere prioritet, og legger seg over tidligere verdier. */
public fun <T> Iterable<T>.somTidslinje(periodeSelector: (T) -> Periode): Tidslinje<T> {
    return this.somTidslinje(periodeSelector, { it })
}

