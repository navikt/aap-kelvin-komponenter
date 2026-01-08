package no.nav.aap.komponenter.verdityper

import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Beløp har ulikt antall desimaler i konstrukør og under utregninger med divisjon.
 *
 * Vær obs hvis man i fremtidig kode velger å returnere divisjonsresultater som Beløp i steden for
 * BigDecimal da det potensielt kan medføre avrundingsfeil i delresultater, se eksempel i enhetstest
 */
public class Beløp(verdi: BigDecimal) {
    public val verdi: BigDecimal = verdi.setScale(ANTALL_DESIMALER, RoundingMode.HALF_UP)

    public constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    public constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))
    public constructor(longVerdi: Long) : this(BigDecimal(longVerdi))

    public companion object {
        public const val ANTALL_DESIMALER: Int = 2
        public const val ANTALL_DESIMALER_I_UTREGNING: Int = 10
        public val AVRUNDINGSMETODE: RoundingMode = RoundingMode.HALF_UP
    }

    @JsonValue
    public fun verdi(): BigDecimal {
        return verdi
    }

    public fun heltallverdi(): BigDecimal {
        return verdi.setScale(0, AVRUNDINGSMETODE)
    }

    public fun pluss(beløp: Beløp): Beløp {
        return Beløp(this.verdi.add(beløp.verdi))
    }

    public fun minus(beløp: Beløp): Beløp {
        return Beløp(this.verdi.subtract(beløp.verdi))
    }

    public fun multiplisert(faktor: Int): Beløp {
        return Beløp(this.verdi.multiply(BigDecimal(faktor)))
    }

    internal fun multiplisert(faktor: BigDecimal): Beløp {
        return Beløp(this.verdi.multiply(faktor))
    }

    public fun multiplisert(faktor: Prosent): Beløp {
        return faktor.multiplisert(this)
    }

    public fun multiplisert(faktor: GUnit): Beløp {
        return faktor.multiplisert(this)
    }

    public fun dividert(nevner: Beløp, scale: Int = ANTALL_DESIMALER_I_UTREGNING): BigDecimal {
        return this.verdi.divide(nevner.verdi, scale, AVRUNDINGSMETODE)
    }

    public fun dividert(nevner: Prosent): Beløp {
        return Beløp(Prosent.dividert(this.verdi, nevner))
    }

    public fun toTredjedeler(): BigDecimal {
        return this.verdi
            .multiply(BigDecimal(2))
            .divide(BigDecimal(3), ANTALL_DESIMALER_I_UTREGNING, AVRUNDINGSMETODE)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Beløp

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }

    override fun toString(): String {
        return "Beløp(verdi=$verdi)"
    }
}
