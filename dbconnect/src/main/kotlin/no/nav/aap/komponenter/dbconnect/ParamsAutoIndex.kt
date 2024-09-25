package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

public class ParamsAutoIndex internal constructor(
    preparedStatement: PreparedStatement,
    connection: Connection
) {
    private val params = Params(preparedStatement, connection)
    private var index = 1
    private fun nextIndex(): Int {
        return index++
    }

    public fun setBytes(bytes: ByteArray?) {
        params.setBytes(nextIndex(), bytes)
    }

    public fun setString(value: String?) {
        params.setString(nextIndex(), value)
    }

    public fun setEnumName(value: Enum<*>?) {
        params.setEnumName(nextIndex(), value)
    }

    public fun setInt(value: Int?) {
        params.setInt(nextIndex(), value)
    }

    public fun setLong(value: Long?) {
        params.setLong(nextIndex(), value)
    }

    public fun setDouble(value: Double?) {
        params.setDouble(nextIndex(), value)
    }

    public fun setBigDecimal(value: BigDecimal?) {
        params.setBigDecimal(nextIndex(), value)
    }

    public fun setUUID(uuid: UUID?) {
        params.setUUID(nextIndex(), uuid)
    }

    public fun setBoolean(value: Boolean?) {
        params.setBoolean(nextIndex(), value)
    }

    public fun setPeriode(periode: Periode?) {
        params.setPeriode(nextIndex(), periode)
    }

    public fun setLocalDate(localDate: LocalDate?) {
        params.setLocalDate(nextIndex(), localDate)
    }

    public fun setLocalDateTime(localDateTime: LocalDateTime?) {
        params.setLocalDateTime(nextIndex(), localDateTime)
    }

    public fun setProperties(properties: Properties?) {
        params.setProperties(nextIndex(), properties)
    }
}
