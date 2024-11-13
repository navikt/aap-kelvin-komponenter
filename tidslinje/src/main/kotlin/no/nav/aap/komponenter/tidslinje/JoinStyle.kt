package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.type.Periode

public interface JoinStyle<VENSTRE, HØYRE, RETUR> {
    public fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>?

    /**
     * Ene eller andre har verdi.
     * ```
     *             venstre høyre  OUTER_JOIN
     * 2020-01-01  +---+          +------------+
     *             | x |          | f(x, null) |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | f(x, 1)    |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | f(null, 1) |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | f(y, null) |
     * 2020-01-06  +---+          +------------+
     * ```
     *
     */
    public class OUTER_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, Segment<VENSTRE>?, Segment<HØYRE>?) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>? {
            if (venstre == null && høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * kun venstre tidsserie.
     *
     * ```
     *             venstre høyre  DISJOINT
     * 2020-01-01  +---+          +------------+
     *             | x |          | f(x)       |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |
     * 2020-01-03  +---+   |   |
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | f(y)       |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    public class DISJOINT<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, Segment<VENSTRE>) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>? {
            if (venstre == null || høyre != null) return null
            return this.kombinerer(periode, venstre)
        }
    }

    /**
     * kun dersom begge tidsserier har verdi.
     *
     * ```
     *             venstre høyre  INNER_JOIN
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | f(x, 1)    |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public class INNER_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, Segment<VENSTRE>, Segment<HØYRE>) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>? {
            if (venstre == null || høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
     * nulls for RHS.
     *
     * ```
     *             venstre høyre  LEFT_JOIN
     * 2020-01-01  +---+          +------------+
     *             | x |          | f(x, null) |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | f(x, 1)    |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | f(y, null) |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    public class LEFT_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, Segment<VENSTRE>, Segment<HØYRE>?) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>? {
            if (venstre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
     * LHS.
     *
     * ```
     *             venstre høyre  RIGHT_JOIN
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | f(x, 1)    |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | f(null, 1) |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public class RIGHT_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, Segment<VENSTRE>?, Segment<HØYRE>) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: Segment<VENSTRE>?, høyre: Segment<HØYRE>?): Segment<RETUR>? {
            if (høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }
}
