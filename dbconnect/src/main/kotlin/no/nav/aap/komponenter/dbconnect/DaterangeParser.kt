package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.temporal.ChronoField

internal object DaterangeParser {

    /**
     * Skreddersydd DateTimeFormatter for å håndtere år fra 0001 til 10_000
     * I Kelvin bruker vi år 0001 som Tid.MIN (f.eks. for å "nullstille" rettighetsperioden).
     * Og fra bl.a. MEDL får vi år 10_000 som makstid (ingen sluttdato).
     **/
    private val formatter = DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.NORMAL)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .appendLiteral('-')
        .appendValue(ChronoField.DAY_OF_MONTH, 2)
        .toFormatter()

    internal fun toSQL(periode: Periode): String {
        return "[${formatter.format(periode.fom)},${formatter.format(periode.tom)}]"
    }

    internal fun fromSQL(daterange: String): Periode {
        val (lower, upper) = daterange.split(",")

        val lowerEnd = lower.first()
        val lowerDate = lower.drop(1)
        val upperDate = upper.dropLast(1)
        val upperEnd = upper.last()

        var fom = formatter.parse(lowerDate, LocalDate::from)
        if (lowerEnd == '(') {
            fom = fom.plusDays(1)
        }

        var tom = formatter.parse(upperDate, LocalDate::from)
        if (upperEnd == ')') {
            tom = tom.minusDays(1)
        }

        return Periode(fom, tom)
    }
}
