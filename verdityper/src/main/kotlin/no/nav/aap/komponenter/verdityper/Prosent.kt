package no.nav.aap.komponenter.verdityper

import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Representerer en prosent-verdi. Kan kun representere heltalls-prosenter og prosenter mellom 0 og 100.
 */
public class Prosent private constructor(verdi: BigDecimal) : Comparable<Prosent> {
    //TODO: Hva skal scale være her? Hvor mange desimaler skal det være i prosenten?
    private val verdi = verdi.setScale(2, RoundingMode.HALF_UP)

    public constructor(intVerdi: Int) : this(BigDecimal(intVerdi).divide(BigDecimal(100), 2, RoundingMode.HALF_UP))

    init {
        require(this.verdi >= BigDecimal(0)) { "Prosent kan ikke være negativ" }
        require(this.verdi <= BigDecimal(1)) { "Prosent kan ikke være større enn 100" }
    }

    @Suppress("ObjectPropertyName")
    public companion object {
        public val `0_PROSENT`: Prosent = Prosent(0)
        public val `30_PROSENT`: Prosent = Prosent(30)
        public val `50_PROSENT`: Prosent = Prosent(50)
        public val `66_PROSENT`: Prosent = Prosent(66)
        public val `70_PROSENT`: Prosent = Prosent(70)
        public val `100_PROSENT`: Prosent = Prosent(100)

        internal fun dividert(teller: BigDecimal, nevner: Prosent, scale: Int = 10): BigDecimal {
            return teller.divide(nevner.verdi, scale, RoundingMode.HALF_UP)
        }

        /** Fra desimal mellom 0 og 1. */
        public fun fraDesimal(andel: BigDecimal): Prosent {
            return Prosent(andel)
        }

        public fun max(first: Prosent, second: Prosent): Prosent {
            return first.verdi.max(second.verdi).let { Prosent(it) }
        }

    }

    @JsonValue
    public fun prosentverdi(): Int {
        return verdi.multiply(BigDecimal(100)).intValueExact()
    }

    /**
     * Om this-verdien er større enn [terskelverdi], rund opp til 100%.
     */
    public fun justertFor(terskelverdi: Prosent): Prosent {
        if (this > terskelverdi) {
            return `100_PROSENT`
        }

        return this
    }

    public fun komplement(): Prosent {
        return `100_PROSENT`.minus(this)
    }

    public fun minus(subtrahend: Prosent, minimumsverdi: Prosent = `0_PROSENT`): Prosent {
        val verdi = this.verdi - subtrahend.verdi
        if (verdi < minimumsverdi.verdi) {
            return minimumsverdi
        }

        return Prosent(verdi)
    }

    public fun multiplisert(faktor: Prosent): Prosent {
        return Prosent(this.verdi.multiply(faktor.verdi))
    }

    internal fun multiplisert(faktor: BigDecimal): BigDecimal {
        return this.verdi.multiply(faktor)
    }

    public fun multiplisert(faktor: Beløp): Beløp {
        return faktor.multiplisert(this.verdi)
    }

    override fun compareTo(other: Prosent): Int {
        return this.verdi.compareTo(other.verdi)
    }

    override fun toString(): String {
        return "Prosent(${verdi * BigDecimal(100)})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Prosent

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }

}
