package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.Period
import java.util.*


public class Tidslinje<T>(initSegmenter: NavigableSet<Segment<T>> = TreeSet()) : Iterable<Segment<T>> {

    public companion object {
        public fun <T> empty(): Tidslinje<T> = Tidslinje<T>(TreeSet())

        public fun <A, B, C> zip3(
            aTidslinje: Tidslinje<A>,
            bTidslinje: Tidslinje<B>,
            cTidslinje: Tidslinje<C>
        ): Tidslinje<Triple<A?, B?, C?>> {
            return aTidslinje
                .kombiner(bTidslinje, JoinStyle.OUTER_JOIN { periode, aSegment, bSegment ->
                    Segment(periode, Pair(aSegment?.verdi, bSegment?.verdi))
                })
                .kombiner(cTidslinje, JoinStyle.OUTER_JOIN { periode, abSegment, cSegment ->
                    Segment(
                        periode,
                        Triple(abSegment?.verdi?.first, abSegment?.verdi?.second, cSegment?.verdi)
                    )
                })
        }
    }

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

    public fun segmenter(): NavigableSet<Segment<T>> {
        return TreeSet(segmenter)
    }

    public fun perioder(): NavigableSet<Periode> {
        return segmenter.mapTo(TreeSet(), Segment<T>::periode)
    }

    /**
     * Merge av to tidslinjer, prioriterer verdier fra den som merges over den som det kalles på
     * oppretter en tredje slik at orginale verdier bevares
     */
    public fun <E, V> kombiner(
        other: Tidslinje<E>,
        joinStyle: JoinStyle<T, E, V>
    ): Tidslinje<V> {
        if (this.segmenter.isEmpty()) {
            val nyeSegmenter = other.segmenter.mapNotNullTo(TreeSet()) { segment ->
                joinStyle.kombiner(segment.periode, null, segment)
            }
            return Tidslinje(nyeSegmenter)
        }
        if (other.segmenter.isEmpty()) {
            val nyeSegmenter = this.segmenter.mapNotNullTo(TreeSet()) { segment ->
                joinStyle.kombiner(segment.periode, segment, null)
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

            val kombinert = joinStyle.kombiner(periode, left, right)
            if (kombinert != null) {
                nySammensetning.add(kombinert)
            }
        }

        return Tidslinje(nySammensetning)
    }

    /**
     * Begrens tidslinjen til [periode].
     */
    public fun disjoint(periode: Periode): Tidslinje<T> {
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

    public fun kryss(periode: Periode): Tidslinje<T> {
        return kombiner(Tidslinje(periode, null), StandardSammenslåere.kunVenstre())
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

    public fun <R> mapValue(mapper: (T) -> R): Tidslinje<R> {
        val newSegments: NavigableSet<Segment<R>> = segmenter.mapTo(TreeSet()) { s ->
            Segment(
                s.periode,
                mapper(s.verdi)
            )
        }
        return Tidslinje(newSegments)
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

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
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

            val nesteSegmenter: NavigableSet<Segment<T>> = kryss(Periode(dt, nextDt.minusDays(1))).segmenter
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
            tidslinjer.add(Segment(p, kryss(p)))
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

    override fun iterator(): Iterator<Segment<T>> {
        return segmenter.iterator()
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
}