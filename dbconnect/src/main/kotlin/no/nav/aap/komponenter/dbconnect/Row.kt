package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Row(private val resultSet: ResultSet) {
    fun getBytes(columnLabel: String): ByteArray {
        val bytes: ByteArray? = getBytesOrNull(columnLabel)
        requireNotNull(bytes)
        return bytes
    }

    fun getBytesOrNull(columnLabel: String): ByteArray? {
        return resultSet.getBytes(columnLabel)
    }

    fun getString(columnLabel: String): String {
        val string: String? = getStringOrNull(columnLabel)
        requireNotNull(string)
        return string
    }

    fun getStringOrNull(columnLabel: String): String? {
        return resultSet.getString(columnLabel)
    }

    inline fun <reified T : Enum<T>> getEnum(columnLabel: String): T {
        return enumValueOf(getString(columnLabel))
    }

    /**
     * Siden enumValueOf ikke kan forholde seg til nullable typer,
     * og compileren ikke kan forstå at typen ikke er null når databaseverdien er null,
     * så innføres [T] for å gi compileren hint om at returtypen kan være null
     */
    inline fun <reified T, reified E> getEnumOrNull(columnLabel: String): E?
            where T : E?,
                  E : Enum<E> {
        return getStringOrNull(columnLabel)?.let<String, E>(::enumValueOf)
    }

    fun getInt(columnLabel: String): Int {
        val int: Int? = getIntOrNull(columnLabel)
        requireNotNull(int)
        return int
    }

    fun getIntOrNull(columnLabel: String): Int? {
        val int = resultSet.getInt(columnLabel)
        if (int != 0) {
            return int
        }

        val any: Any? = resultSet.getObject(columnLabel)
        if (any == null) {
            return null
        }

        return 0
    }

    fun getLong(columnLabel: String): Long {
        val long: Long? = getLongOrNull(columnLabel)
        requireNotNull(long)
        return long
    }

    fun getLongOrNull(columnLabel: String): Long? {
        val long = resultSet.getLong(columnLabel)
        if (long != 0L) {
            return long
        }

        val any: Any? = resultSet.getObject(columnLabel)
        if (any == null) {
            return null
        }

        return 0L
    }

    fun getBigDecimal(columnLabel: String): BigDecimal {
        val bigDecimal = getBigDecimalOrNull(columnLabel)
        requireNotNull(bigDecimal)
        return bigDecimal
    }

    fun getBigDecimalOrNull(columnLabel: String): BigDecimal? {
        val bigDecimal: BigDecimal? = resultSet.getBigDecimal(columnLabel)
        return bigDecimal
    }

    fun getUUID(columnLabel: String): UUID {
        return UUID.fromString(getString(columnLabel))
    }

    fun getUUIDOrNull(columnLabel: String): UUID? {
        val string = getStringOrNull(columnLabel)
        if (string == null) {
            return null
        }
        return UUID.fromString(string)
    }

    fun getBoolean(columnLabel: String): Boolean {
        val boolean = getBooleanOrNull(columnLabel)
        requireNotNull(boolean)
        return boolean
    }

    fun getBooleanOrNull(columnLabel: String): Boolean? {
        val boolean = resultSet.getBoolean(columnLabel)
        if (boolean) {
            return true
        }

        val any: Any? = resultSet.getObject(columnLabel)
        if (any == null) {
            return null
        }

        return false
    }

    fun getPeriode(columnLabel: String): Periode {
        return DaterangeParser.fromSQL(getString(columnLabel))
    }

    fun getPeriodeOrNull(columnLabel: String): Periode? {
        val dateRange = getStringOrNull(columnLabel)
        if (dateRange == null) {
            return null
        }
        return DaterangeParser.fromSQL(dateRange)
    }

    fun getLocalDate(columnLabel: String): LocalDate {
        val localDate = getLocalDateOrNull(columnLabel)
        requireNotNull(localDate)
        return localDate
    }

    fun getLocalDateOrNull(columnLabel: String): LocalDate? {
        val date: Date? = resultSet.getDate(columnLabel)
        return date?.toLocalDate()
    }

    fun getLocalDateTime(columnLabel: String): LocalDateTime {
        val localDateTime = getLocalDateTimeOrNull(columnLabel)
        requireNotNull(localDateTime)
        return localDateTime
    }

    fun getLocalDateTimeOrNull(columnLabel: String): LocalDateTime? {
        val timestamp: Timestamp? = resultSet.getTimestamp(columnLabel)
        return timestamp?.toLocalDateTime()
    }

    fun getPropertiesOrNull(columnLabel: String): Properties? {
        val dbData = resultSet.getString(columnLabel)
        return PropertiesParser.fromSql(dbData)
    }

    fun getProperties(columnLabel: String): Properties {
        val propertiesOrNull = getPropertiesOrNull(columnLabel)
        requireNotNull(propertiesOrNull)
        return propertiesOrNull
    }
}
