package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ParamsAutoIndex(preparedStatement: PreparedStatement) {
    private val params = Params(preparedStatement)
    private var index = 1
    private fun nextIndex(): Int {
        return index++
    }

    fun setBytes(bytes: ByteArray?) {
        params.setBytes(nextIndex(), bytes)
    }

    fun setString(value: String?) {
        params.setString(nextIndex(), value)
    }

    fun setEnumName(value: Enum<*>?) {
        params.setEnumName(nextIndex(), value)
    }

    fun setInt(value: Int?) {
        params.setInt(nextIndex(), value)
    }

    fun setLong(value: Long?) {
        params.setLong(nextIndex(), value)
    }

    fun setBigDecimal(value: BigDecimal?) {
        params.setBigDecimal(nextIndex(), value)
    }

    fun setUUID(uuid: UUID?) {
        params.setUUID(nextIndex(), uuid)
    }

    fun setBoolean(value: Boolean?) {
        params.setBoolean(nextIndex(), value)
    }

    fun setPeriode(periode: Periode?) {
        params.setPeriode(nextIndex(), periode)
    }

    fun setLocalDate(localDate: LocalDate?) {
        params.setLocalDate(nextIndex(), localDate)
    }

    fun setLocalDateTime(localDateTime: LocalDateTime?) {
        params.setLocalDateTime(nextIndex(), localDateTime)
    }

    fun setProperties(properties: Properties?) {
        params.setProperties(nextIndex(), properties)
    }
}
