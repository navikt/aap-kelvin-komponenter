package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Tidspunkt
import no.nav.aap.komponenter.verdityper.Bruker
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

/**
 * Typed JDBC parameter setters. Use an index for `?` placeholders or a name for `:name` placeholders.
 * Named setters bind every occurrence of the name, regardless of setter order.
 */
public class Params internal constructor(
    private val preparedStatement: PreparedStatement,
    private val connection: Connection,
    private val namedIndexes: Map<String, List<Int>> = emptyMap()
) {
    private val boundNames = mutableSetOf<String>()

    private fun bind(name: String, setter: (Int) -> Unit) {
        val indexes = requireNotNull(namedIndexes[name]) { "Unknown SQL parameter: $name" }
        indexes.forEach(setter)
        boundNames.add(name)
    }

    internal fun validateNamedParameters() {
        val missing = namedIndexes.keys - boundNames
        require(missing.isEmpty()) { "Missing SQL parameters: ${missing.joinToString()}" }
    }

    public fun setBytes(name: String, bytes: ByteArray?): Unit = bind(name) { setBytes(it, bytes) }
    public fun setString(name: String, value: String?): Unit = bind(name) { setString(it, value) }
    public fun setEnumName(name: String, value: Enum<*>?): Unit = bind(name) { setEnumName(it, value) }
    public fun setInt(name: String, value: Int?): Unit = bind(name) { setInt(it, value) }
    public fun setLong(name: String, value: Long?): Unit = bind(name) { setLong(it, value) }
    public fun setDouble(name: String, value: Double?): Unit = bind(name) { setDouble(it, value) }
    public fun setBigDecimal(name: String, value: BigDecimal?): Unit = bind(name) { setBigDecimal(it, value) }
    public fun setUUID(name: String, uuid: UUID?): Unit = bind(name) { setUUID(it, uuid) }
    public fun setBoolean(name: String, value: Boolean?): Unit = bind(name) { setBoolean(it, value) }
    public fun setPeriode(name: String, periode: Periode?): Unit = bind(name) { setPeriode(it, periode) }
    public fun setLocalDate(name: String, localDate: LocalDate?): Unit = bind(name) { setLocalDate(it, localDate) }
    public fun setLocalDateTime(name: String, localDateTime: LocalDateTime?): Unit =
        bind(name) { setLocalDateTime(it, localDateTime) }
    public fun setInstant(name: String, instant: Instant?): Unit = bind(name) { setInstant(it, instant) }
    public fun setTidspunkt(name: String, tidspunkt: Tidspunkt?): Unit = bind(name) { setTidspunkt(it, tidspunkt) }
    public fun setProperties(name: String, properties: Properties?): Unit = bind(name) { setProperties(it, properties) }
    public fun setBruker(name: String, bruker: Bruker?): Unit = bind(name) { setBruker(it, bruker) }
    public fun setArray(name: String, strings: List<String>): Unit = bind(name) { setArray(it, strings) }
    public fun setLongArray(name: String, longs: List<Long>): Unit = bind(name) { setLongArray(it, longs) }
    public fun setPeriodeArray(name: String, perioder: List<Periode>?): Unit = bind(name) { setPeriodeArray(it, perioder) }
    public fun setUUIDArray(name: String, uuids: List<UUID>): Unit = bind(name) { setUUIDArray(it, uuids) }

    public fun setBytes(index: Int, bytes: ByteArray?) {
        preparedStatement.setBytes(index, bytes)
    }

    public fun setString(index: Int, value: String?) {
        preparedStatement.setString(index, value)
    }

    public fun setEnumName(index: Int, value: Enum<*>?) {
        preparedStatement.setString(index, value?.name)
    }

    public fun setInt(index: Int, value: Int?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.SMALLINT)
        } else {
            preparedStatement.setInt(index, value)
        }
    }

    public fun setLong(index: Int, value: Long?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.NUMERIC)
        } else {
            preparedStatement.setLong(index, value)
        }
    }

    public fun setDouble(index: Int, value: Double?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.DOUBLE)
        } else {
            preparedStatement.setDouble(index, value)
        }
    }

    public fun setBigDecimal(index: Int, value: BigDecimal?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.NUMERIC)
        } else {
            preparedStatement.setBigDecimal(index, value)
        }
    }

    public fun setUUID(index: Int, uuid: UUID?) {
        preparedStatement.setObject(index, uuid)
    }

    public fun setBoolean(index: Int, value: Boolean?) {
        if (value == null) {
            preparedStatement.setNull(index, Types.BOOLEAN)
        } else {
            preparedStatement.setBoolean(index, value)
        }
    }

    public fun setPeriode(index: Int, periode: Periode?) {
        preparedStatement.setString(index, periode?.let(DaterangeParser::toSQL))
    }

    public fun setLocalDate(index: Int, localDate: LocalDate?) {
        preparedStatement.setDate(index, localDate?.let(Date::valueOf))
    }

    public fun setLocalDateTime(index: Int, localDateTime: LocalDateTime?) {
        preparedStatement.setTimestamp(index, localDateTime?.let(Timestamp::valueOf))
    }

    public fun setInstant(index: Int, instant: Instant?) {
        preparedStatement.setTimestamp(index, instant?.let(Timestamp::from))
    }

    public fun setTidspunkt(index: Int, tidspunkt: Tidspunkt?) {
        preparedStatement.setTimestamp(index, tidspunkt?.let { Timestamp.from(it.asInstant) })
    }

    public fun setProperties(index: Int, properties: Properties?) {
        preparedStatement.setString(index, PropertiesParser.toSql(properties))
    }

    public fun setBruker(index: Int, bruker: Bruker?) {
        setString(index, bruker?.ident)
    }

    /**
     * Bruk følgende syntaks i queryen
     * eks: WHERE TEST = ANY(?::text[])
     * */
    public fun setArray(index: Int, strings: List<String>) {
        val array = connection.createArrayOf("VARCHAR", strings.toTypedArray())
        preparedStatement.setArray(index, array)
    }

    public fun setLongArray(index: Int, longs: List<Long>) {
        val array = connection.createArrayOf("BIGINT", longs.toTypedArray())
        preparedStatement.setArray(index, array)
    }

    public fun setPeriodeArray(index: Int, perioder: List<Periode>?) {
        val array = perioder?.let{connection.createArrayOf("daterange", it.map(DaterangeParser::toSQL).toTypedArray())}
        preparedStatement.setArray(index, array)
    }

    public fun setUUIDArray(index: Int, longs: List<UUID>) {
        val array = connection.createArrayOf("UUID", longs.toTypedArray())
        preparedStatement.setArray(index, array)
    }
}
