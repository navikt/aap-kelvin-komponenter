package no.nav.aap.komponenter.verdityper

import java.math.BigDecimal

public data class TimerArbeid(val antallTimer: BigDecimal) {
    init {
        require(antallTimer >= BigDecimal.ZERO) { "Kan ikke jobbe mindre enn 0 timer" }
    }
}
