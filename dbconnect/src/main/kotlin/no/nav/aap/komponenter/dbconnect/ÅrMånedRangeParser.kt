package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.ÅrMånedPeriode
import java.time.YearMonth
import java.time.format.DateTimeFormatter

internal object ÅrMånedRangeParser {

    private val formater = DateTimeFormatter.ofPattern("y-MM")

    internal fun toSQL(årMånedPeriode: ÅrMånedPeriode): String {
        return "[${formater.format(årMånedPeriode.fom)},${formater.format(årMånedPeriode.tom)}]"
    }

    internal fun fromSQL(årMånedRange: String): ÅrMånedPeriode {
        val (lower, upper) = årMånedRange.split(",")

        val lowerEnd = lower.first()
        val lowerDate = lower.drop(1)
        val upperDate = upper.dropLast(1)
        val upperEnd = upper.last()

        var fom = formater.parse(lowerDate, YearMonth::from)
        if (lowerEnd == '(') {
            fom = fom.plusMonths(1)
        }

        var tom = formater.parse(upperDate, YearMonth::from)
        if (upperEnd == ')') {
            tom = tom.minusMonths(1)
        }

        return ÅrMånedPeriode(fom, tom)
    }
}
