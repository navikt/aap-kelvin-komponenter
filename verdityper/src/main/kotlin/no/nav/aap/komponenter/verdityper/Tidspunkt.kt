package no.nav.aap.komponenter.verdityper

import com.fasterxml.jackson.annotation.JsonValue
import no.nav.aap.komponenter.verdityper.Tid.norskTidssone
import java.io.Serializable
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalField
import java.time.temporal.TemporalQuery
import java.time.temporal.TemporalUnit
import java.time.temporal.ValueRange

/** Et tidspunkt, tilsvarende Javas [instant][java.time.Instant], men begrenset til verdier som Postgres kan lagre.
 *
 * Javas [Instant][java.time.Instant] kan lagre nanosekunder, mens Postgres' timestamp kan lagre mikrosekunder. Denne
 * klassen skal fungere tilsvarende som en Instant, men bare med mikrosekunder, slik at vi får
 * 1-til-1 mellom kode og database.
 */
public class Tidspunkt
private constructor(
    @JsonValue public val asInstant: Instant,
) : Temporal by asInstant,
    TemporalAdjuster,
    Comparable<Tidspunkt>,
    Serializable by asInstant {

    init {
        require(asInstant.truncatedTo(unit) == asInstant) {
            """Privat konstruktør for Tidspunkt kalt med instant ($asInstant) som mangler trunkering til microsekunder."""
        }
        require(MIN_AS_INSTANT <= asInstant) {
            """Privat konstruktør for Tidspunkt kalt med instant ($asInstant) som tidligere enn MIN-tidspunktet ($MIN_AS_INSTANT)."""
        }
        require(asInstant <= MAX_AS_INSTANT) {
            """Privat konstruktør for Tidspunkt kalt med instant ($asInstant) som senere enn MAX-tidspunktet ($MAX_AS_INSTANT)."""
        }
    }

    public companion object {
        private val unit: ChronoUnit = ChronoUnit.MICROS

        /* Dette tilsvarer Postgres' min og max. Kan vurdere å harmonisere med Tid.MIN og Tid.MAKS. */
        private val MIN_AS_INSTANT = Instant.parse("-4713-10-16T23:00:00.000000Z")
        private val MAX_AS_INSTANT = Instant.parse("+294276-12-31T22:59:59.999999Z")

        public fun now(clock: Clock = Clock.systemDefaultZone()): Tidspunkt =
            ofInstant(Instant.now(clock))

        public fun ofInstant(instant: Instant): Tidspunkt =
            Tidspunkt(instant.truncatedTo(unit).coerceIn(MIN_AS_INSTANT, MAX_AS_INSTANT))

        public fun ofTemporal(temporal: Temporal): Tidspunkt =
            ofInstant(Instant.from(temporal))

        public fun parse(text: String): Tidspunkt =
            ofInstant(Instant.parse(text))

        public fun ofLocalDateTimeNorskTid(tidspunkt: LocalDateTime): Tidspunkt =
            ofInstant(tidspunkt.atZone(norskTidssone).toInstant())
    }

    public fun toZonedDateTimeNorskTid(): ZonedDateTime =
        asInstant.atZone(norskTidssone)

    public fun toLocalDateTimeNorskTid(): LocalDateTime =
        toZonedDateTimeNorskTid().toLocalDateTime()

    public fun toLocalDateNorskTid(): LocalDate =
        toZonedDateTimeNorskTid().toLocalDate()

    public fun toLocalTimeNorskTid(): LocalTime =
        toZonedDateTimeNorskTid().toLocalTime()

    override fun toString(): String = asInstant.toString()

    override fun compareTo(other: Tidspunkt): Int = this.asInstant.compareTo(other.asInstant)

    override fun equals(other: Any?): Boolean = when (other) {
        is Tidspunkt -> this.asInstant == other.asInstant
        else -> false
    }

    override fun hashCode(): Int = asInstant.hashCode()

    override fun isSupported(field: TemporalField?): Boolean = asInstant.isSupported(field)

    override fun isSupported(unit: TemporalUnit?): Boolean = asInstant.isSupported(unit)

    override fun range(field: TemporalField?): ValueRange? = asInstant.range(field)

    override fun get(field: TemporalField?): Int = asInstant.get(field)

    override fun <R> query(query: TemporalQuery<R?>?): R? = asInstant.query(query)

    override fun with(adjuster: TemporalAdjuster): Tidspunkt =
        ofInstant(asInstant.with(adjuster))

    override fun with(field: TemporalField, newValue: Long): Tidspunkt =
        ofInstant(asInstant.with(field, newValue))

    override fun plus(amount: TemporalAmount): Tidspunkt =
        ofInstant(asInstant.plus(amount))

    override fun plus(amountToAdd: Long, unit: TemporalUnit): Tidspunkt =
        ofInstant(asInstant.plus(amountToAdd, unit))

    override fun minus(amount: TemporalAmount?): Tidspunkt =
        ofInstant(asInstant.minus(amount))

    override fun minus(amountToSubtract: Long, unit: TemporalUnit): Tidspunkt =
        ofInstant(asInstant.minus(amountToSubtract, unit))

    override fun adjustInto(temporal: Temporal): Tidspunkt =
        ofTemporal(asInstant.adjustInto(temporal))
}