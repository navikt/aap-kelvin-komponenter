package no.nav.aap.komponenter.verdityper

import java.time.LocalDate
import java.time.Month

public object Tid {
    public val MIN: LocalDate = LocalDate.of(1, Month.JANUARY, 1)
    public val MAKS: LocalDate = LocalDate.of(2999, Month.JANUARY, 1)
}