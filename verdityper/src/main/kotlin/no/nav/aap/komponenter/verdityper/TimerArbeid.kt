package no.nav.aap.komponenter.verdityper

import java.math.BigDecimal

public class TimerArbeid(antallTimer: BigDecimal) {
    init {
        require(antallTimer >= BigDecimal.ZERO) { "Kan ikke jobbe mindre enn 0 timer" }
    }

    //TODO - holder det med 1 desimal her? Har man lyst til å alltid runde til gunstigst istedet?
    public val antallTimer: BigDecimal = antallTimer.setScale(1, java.math.RoundingMode.HALF_UP)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TimerArbeid

        return antallTimer == other.antallTimer
    }

    override fun hashCode(): Int {
        return antallTimer.hashCode()
    }
}
