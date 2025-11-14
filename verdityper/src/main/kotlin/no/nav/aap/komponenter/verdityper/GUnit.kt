package no.nav.aap.komponenter.verdityper

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Representerer et beløp som faktor av antall G for en tidsuavhengig representasjon
 * av beløp som skal G-justeres.
 */
public class GUnit(verdi: BigDecimal) : Comparable<GUnit> {
    private val verdi = verdi.setScale(ANTALL_DESIMALER, AVRUNDINGSMETODE)

    public constructor(intVerdi: Int) : this(BigDecimal(intVerdi))
    public constructor(stringVerdi: String) : this(BigDecimal(stringVerdi))

    public companion object {
        @Deprecated("Benytt ANTALL_DESIMALER for enklere forståelse")
        public const val SCALE: Int = 10
        public const val ANTALL_DESIMALER: Int = 10
        public val AVRUNDINGSMETODE: RoundingMode = RoundingMode.HALF_UP

        /**
         * Gitt liste med [GUnit], returner gjennomsnittsverdien.
         */
        public fun gjennomsnittlig(gUnits: List<GUnit>): GUnit {
            val gjennomsnitt = gUnits.summer()
            return GUnit(gjennomsnitt.verdi.divide(BigDecimal(gUnits.size), AVRUNDINGSMETODE))
        }

        private fun Iterable<GUnit>.summer(): GUnit {
            return GUnit(this.sumOf { gUnit -> gUnit.verdi })
        }
    }

    public fun verdi(): BigDecimal {
        return verdi
    }

    public fun pluss(addend: GUnit): GUnit {
        return GUnit(this.verdi + addend.verdi)
    }

    public fun multiplisert(faktor: Prosent): GUnit {
        return GUnit(faktor.multiplisert(this.verdi))
    }

    public fun multiplisert(faktor: Beløp): Beløp {
        return faktor.multiplisert(this.verdi)
    }

    public fun multiplisert(faktor: Int): GUnit {
        return GUnit(this.verdi.multiply(BigDecimal(faktor)))
    }

    public fun dividert(nevner: Prosent): GUnit {
        return GUnit(
            Prosent.dividert(
                teller = this.verdi,
                nevner = nevner,
                scale = ANTALL_DESIMALER
            )
        )
    }

    public fun dividert(nevner: Int): GUnit {
        return GUnit(this.verdi.divide(BigDecimal(nevner), ANTALL_DESIMALER, AVRUNDINGSMETODE))
    }

    public fun toTredjedeler(): GUnit {
        return this.multiplisert(2).dividert(3)
    }

    /**
     * Begrenser beløpet til 6G, det samme som `min(beløp, 6G)`.
     */
    public fun begrensTil6GUnits(): GUnit {
        val begrensetVerdi = minOf(verdi, BigDecimal(6))
        return GUnit(begrensetVerdi)
    }

    override fun compareTo(other: GUnit): Int {
        return this.verdi.compareTo(other.verdi)
    }

    override fun toString(): String {
        return "GUnit(verdi=$verdi)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GUnit

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }
}
