package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
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

public class Params internal constructor(
    private val preparedStatement: PreparedStatement,
    private val connection: Connection
) {
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

    public fun setProperties(index: Int, properties: Properties?) {
        preparedStatement.setString(index, PropertiesParser.toSql(properties))
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

    public fun setPeriodeArray(index: Int, perioder: List<Periode>) {
        val array = connection.createArrayOf("daterange", perioder.map(DaterangeParser::toSQL).toTypedArray())
        preparedStatement.setArray(index, array)
    }
}
