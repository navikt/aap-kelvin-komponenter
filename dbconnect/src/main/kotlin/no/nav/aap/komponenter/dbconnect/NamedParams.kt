package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

public class NamedParams internal constructor(
    preparedStatement: PreparedStatement,
    connection: Connection,
    private val queryparser: Queryparser
) {
    private val params = Params(preparedStatement, connection)

    public fun setBytes(name: String, bytes: ByteArray?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBytes(index, bytes)
        }
    }

    public fun setString(name: String, value: String?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setString(index, value)
        }
    }

    public fun setEnumName(name: String, value: Enum<*>?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setEnumName(index, value)
        }
    }

    public fun setInt(name: String, value: Int?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setInt(index, value)
        }
    }

    public fun setLong(name: String, value: Long?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLong(index, value)
        }
    }

    public fun setDouble(name: String, value: Double?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setDouble(index, value)
        }
    }

    public fun setBigDecimal(name: String, value: BigDecimal?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBigDecimal(index, value)
        }
    }

    public fun setUUID(name: String, uuid: UUID?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setUUID(index, uuid)
        }
    }

    public fun setBoolean(name: String, value: Boolean?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBoolean(index, value)
        }
    }

    public fun setPeriode(name: String, periode: Periode?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setPeriode(index, periode)
        }
    }

    public fun setLocalDate(name: String, localDate: LocalDate?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLocalDate(index, localDate)
        }
    }

    public fun setLocalDateTime(name: String, localDateTime: LocalDateTime?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLocalDateTime(index, localDateTime)
        }
    }

    public fun setProperties(name: String, properties: Properties?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setProperties(index, properties)
        }
    }
}
