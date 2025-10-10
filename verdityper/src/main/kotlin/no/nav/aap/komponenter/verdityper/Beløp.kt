package no.nav.aap.komponenter.verdityper

import java.math.BigDecimal
import java.math.RoundingMode

public class Beløp(verdi: BigDecimal) {
    public val verdi: BigDecimal = verdi.setScale(2, RoundingMode.HALF_UP)

    public constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    public constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))
    public constructor(longVerdi: Long) : this(BigDecimal(longVerdi))

    public fun verdi(): BigDecimal {
        return verdi
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

    public fun dividert(nevner: Beløp, scale: Int = 10): BigDecimal {
        return this.verdi.divide(nevner.verdi, scale, RoundingMode.HALF_UP)
    }

    public fun dividert(nevner: Prosent): Beløp {
        return Beløp(Prosent.dividert(this.verdi, nevner))
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
