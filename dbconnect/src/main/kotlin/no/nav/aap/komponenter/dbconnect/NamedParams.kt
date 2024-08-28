package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class NamedParams internal constructor(
    preparedStatement: PreparedStatement,
    private val queryparser: Queryparser
) {
    private val params = Params(preparedStatement)

    fun setBytes(name: String, bytes: ByteArray?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBytes(index, bytes)
        }
    }

    fun setString(name: String, value: String?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setString(index, value)
        }
    }

    fun setEnumName(name: String, value: Enum<*>?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setEnumName(index, value)
        }
    }

    fun setInt(name: String, value: Int?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setInt(index, value)
        }
    }

    fun setLong(name: String, value: Long?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLong(index, value)
        }
    }

    fun setBigDecimal(name: String, value: BigDecimal?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBigDecimal(index, value)
        }
    }

    fun setUUID(name: String, uuid: UUID?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setUUID(index, uuid)
        }
    }

    fun setBoolean(name: String, value: Boolean?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setBoolean(index, value)
        }
    }

    fun setPeriode(name: String, periode: Periode?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setPeriode(index, periode)
        }
    }

    fun setLocalDate(name: String, localDate: LocalDate?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLocalDate(index, localDate)
        }
    }

    fun setLocalDateTime(name: String, localDateTime: LocalDateTime?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setLocalDateTime(index, localDateTime)
        }
    }

    fun setProperties(name: String, properties: Properties?) {
        queryparser.getIndices(name)?.forEach { index ->
            params.setProperties(index, properties)
        }
    }
}
