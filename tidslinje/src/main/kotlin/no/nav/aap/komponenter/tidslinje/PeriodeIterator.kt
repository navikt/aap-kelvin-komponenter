package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode
import java.util.*

public class PeriodeIterator(
    leftPerioder: Iterable<Periode>,
    rightPerioder: Iterable<Periode>
) : Iterator<Periode> {

    private var unikePerioder: NavigableSet<Periode> = TreeSet()
    private var dateIterator: Iterator<Periode>

    init {
        val temp = TreeSet(leftPerioder + rightPerioder)
        // Kan traverse lineær, siden TreeSet allerede har sortert.
        // Se https://stackoverflow.com/a/9775727/1013553
        // for algoritmen.
        unikePerioder = temp.fold(unikePerioder) { acc, curr ->
            // At beginning
            if (acc.isEmpty()) {
                acc.add(curr)
                acc
            } else {
                val last = acc.last
                if (acc.last.overlapper(curr)) {
                    acc.remove(last)
                    acc.addAll(last.minus(curr))
                    acc.add(curr.overlapp(last))
                    acc.addAll(curr.minus(last))
                } else {
                    acc.add(curr)
                }
                acc
            }
        }

        dateIterator = unikePerioder.iterator()
    }

    override fun hasNext(): Boolean {
        return dateIterator.hasNext()
    }

    override fun next(): Periode {
        return dateIterator.next()
    }
}