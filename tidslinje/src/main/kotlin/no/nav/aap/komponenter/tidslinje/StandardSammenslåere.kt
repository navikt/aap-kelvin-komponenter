package no.nav.aap.komponenter.tidslinje

import no.nav.aap.komponenter.verdityper.Beløp
import java.math.BigDecimal

public object StandardSammenslåere {
    public fun summerer(): JoinStyle.OUTER_JOIN<Beløp, Beløp, Beløp> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            val høyreVerdi = høyreSegment?.verdi ?: Beløp(BigDecimal.ZERO)
            val venstreVerdi = venstreSegment?.verdi ?: Beløp(BigDecimal.ZERO)

            Segment(periode, høyreVerdi.pluss(venstreVerdi))
        }
    }

    /**
     * ```
     *             venstre høyre  priorterHøyreSide
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public fun <T : Any> prioriterHøyreSide(): JoinStyle.INNER_JOIN<T, T, T> {
        return JoinStyle.INNER_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  priorterHøyreSideCrossJoin
     * 2020-01-01  +---+          +------------+
     *             | x |          | x          |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | y          |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    public fun <T> prioriterHøyreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstre, høyre ->
            if (høyre != null) return@OUTER_JOIN Segment(periode, høyre.verdi)
            if (venstre == null) return@OUTER_JOIN null
            Segment(periode, venstre.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  priorterVenstreSideCrossJoin
     * 2020-01-01  +---+          +------------+
     *             | x |          | x          |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | x          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | y          |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    public fun <T> prioriterVenstreSideCrossJoin(): JoinStyle.OUTER_JOIN<T, T, T> {
        return JoinStyle.OUTER_JOIN { periode, venstreSegment, høyreSegment ->
            if (venstreSegment != null) return@OUTER_JOIN Segment(periode, venstreSegment.verdi)
            if (høyreSegment == null) return@OUTER_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunVenstre
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | x          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public fun <T, S> kunVenstre(): JoinStyle.INNER_JOIN<T, S, T> {
        return JoinStyle.INNER_JOIN { periode, venstreSegment, _ ->
            Segment(periode, venstreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  minus
     * 2020-01-01  +---+          +------------+
     *             | x |          | x          |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |
     * 2020-01-03  +---+   |   |
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+          +------------+
     *             | y |          | y          |
     * 2020-01-06  +---+          +------------+
     * ```
     */
    public fun <T, S> minus(): JoinStyle.LEFT_JOIN<T, S, T> {
        return JoinStyle.LEFT_JOIN { p, l, r ->
            if (r == null) Segment(p, l.verdi) else null
        }
    }

    /**
     * ```
     *             venstre høyre  kunHøyreLeftJoin
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    @Deprecated("Bruk [kunHøyre]", ReplaceWith("StandardSammenslåere.kunHøyre()"))
    public fun <T, S> kunHøyreLeftJoin(): JoinStyle.LEFT_JOIN<S, T, T> {
        return JoinStyle.LEFT_JOIN { periode, _, høyreSegment ->
            if (høyreSegment == null) return@LEFT_JOIN null
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunHøyre
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |
     * 2020-01-04          +---+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public fun <T, S> kunHøyre(): JoinStyle.INNER_JOIN<S, T, T> {
        return JoinStyle.INNER_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }

    /**
     * ```
     *             venstre høyre  kunHøyreRightJoin
     * 2020-01-01  +---+
     *             | x |
     * 2020-01-02  |   |   +---+  +------------+
     *             |   |   | 1 |  | 1          |
     * 2020-01-03  +---+   |   |  +------------+
     *                     |   |  | 1          |
     * 2020-01-04          +---+  +------------+
     *
     * 2020-01-05  +---+
     *             | y |
     * 2020-01-06  +---+
     * ```
     */
    public fun <T, S> kunHøyreRightJoin(): JoinStyle.RIGHT_JOIN<S, T, T> {
        return JoinStyle.RIGHT_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment.verdi)
        }
    }

    public fun <T> xor(): JoinStyle<T ,T , T> {
        return JoinStyle.OUTER_JOIN { p, venstreSegment, høyreSegment ->
            when {
                venstreSegment == null && høyreSegment != null -> høyreSegment
                venstreSegment != null && høyreSegment == null -> venstreSegment
                else -> error("tidslinjene overlapper")
            }
        }
    }
}