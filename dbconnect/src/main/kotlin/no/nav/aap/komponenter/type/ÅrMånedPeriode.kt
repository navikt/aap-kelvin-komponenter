package no.nav.aap.komponenter.type

import java.time.YearMonth

public class ÅrMånedPeriode(public val fom: YearMonth, public val tom: YearMonth) : Comparable<ÅrMånedPeriode> {
    
    init {
        require(fom <= tom) { "tom($tom) er før fom($fom)" }
    }
    
    override fun compareTo(other: ÅrMånedPeriode): Int {
        val compareFom = fom.compareTo(other.fom)

        if (compareFom != 0) {
            return compareFom
        }

        return tom.compareTo(other.tom)
    }

    public fun jsonValue(): String {
        return "$fom/$tom"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ÅrMånedPeriode) return false

        if (fom != other.fom) return false
        if (tom != other.tom) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    override fun toString(): String {
        return "MånedPeriode(fom=$fom, tom=$tom)"
    }
    
    public fun tilPeriode(): Periode {
        return Periode(fom.atDay(1), tom.atEndOfMonth())
    }

}