package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.type.Periode
import java.math.BigDecimal
import java.sql.Date
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KClass

public class Row internal constructor(private val resultSet: ResultSet) {
    public fun getBytes(columnLabel: String): ByteArray {
        val bytes: ByteArray? = getBytesOrNull(columnLabel)
        requireNotNull(bytes) { "Null value when retrieving column $columnLabel." }
        return bytes
    }

    public fun getBytesOrNull(columnLabel: String): ByteArray? {
        return resultSet.getBytes(columnLabel)
    }

    public fun getString(columnLabel: String): String {
        val string: String? = getStringOrNull(columnLabel)
        requireNotNull(string) { "Null value when retrieving column $columnLabel." }
        return string
    }

    public fun getStringOrNull(columnLabel: String): String? {
        return resultSet.getString(columnLabel)
    }

    public inline fun <reified T : Enum<T>> getEnum(columnLabel: String): T {
        return enumValueOf(getString(columnLabel))
    }

    /**
     * Siden enumValueOf ikke kan forholde seg til nullable typer,
     * og compileren ikke kan forstå at typen ikke er null når databaseverdien er null,
     * så innføres [T] for å gi compileren hint om at returtypen kan være null
     */
    public inline fun <reified T, reified E> getEnumOrNull(columnLabel: String): E?
            where T : E?,
                  E : Enum<E> {
        return getStringOrNull(columnLabel)?.let<String, E>(::enumValueOf)
    }

    public fun getInt(columnLabel: String): Int {
        val int: Int? = getIntOrNull(columnLabel)
        requireNotNull(int) { "Null value when retrieving column $columnLabel." }
        return int
    }

    public fun getIntOrNull(columnLabel: String): Int? {
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

    public fun getLong(columnLabel: String): Long {
        val long: Long? = getLongOrNull(columnLabel)
        requireNotNull(long) { "Null-value for column label $columnLabel." }
        return long
    }

    public fun getDoubleOrNull(columnLabel: String): Double? {
        val double = resultSet.getDouble(columnLabel)
        if (double != 0.0) {
            return double
        }

        val any: Any? = resultSet.getObject(columnLabel)
        if (any == null) {
            return null
        }

        return 0.0
    }

    public fun getDouble(columnLabel: String): Double {
        val double: Double? = getDoubleOrNull(columnLabel)
        requireNotNull(double) { "Null-value for column label $columnLabel." }
        return double
    }

    public fun getLongOrNull(columnLabel: String): Long? {
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

    public fun getBigDecimal(columnLabel: String): BigDecimal {
        val bigDecimal = getBigDecimalOrNull(columnLabel)
        requireNotNull(bigDecimal) { "Null-value for column label $columnLabel." }
        return bigDecimal
    }

    public fun getBigDecimalOrNull(columnLabel: String): BigDecimal? {
        val bigDecimal: BigDecimal? = resultSet.getBigDecimal(columnLabel)
        return bigDecimal
    }

    public fun getUUID(columnLabel: String): UUID {
        return UUID.fromString(getString(columnLabel))
    }

    public fun getUUIDOrNull(columnLabel: String): UUID? {
        val string = getStringOrNull(columnLabel)
        if (string == null) {
            return null
        }
        return UUID.fromString(string)
    }

    public fun getBoolean(columnLabel: String): Boolean {
        val boolean = getBooleanOrNull(columnLabel)
        requireNotNull(boolean) { "Null-value for column label $columnLabel." }
        return boolean
    }

    public fun getBooleanOrNull(columnLabel: String): Boolean? {
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

    public fun getPeriode(columnLabel: String): Periode {
        return DaterangeParser.fromSQL(getString(columnLabel))
    }

    public fun getPeriodeOrNull(columnLabel: String): Periode? {
        val dateRange = getStringOrNull(columnLabel)
        if (dateRange == null) {
            return null
        }
        return DaterangeParser.fromSQL(dateRange)
    }

    public fun getPeriodeArray(columnLabel: String): List<Periode> {
        return (resultSet.getArray(columnLabel).array as Array<*>)
            .map { DaterangeParser.fromSQL(it.toString()) }
    }

    /**
     * Feltet [elementType] må være av en type JDBC returnerer.
     * Example:
     * ```kotlin
     * row.getArray("my_array", Int::class)
     * ```
     * */
    public fun <E : Any> getArray(columnLabel: String, elementType: KClass<E>): List<E> {
        val array = resultSet.getArray(columnLabel) ?: return emptyList()
        val sqlArray = array.array as? Array<*>
        return sqlArray?.filterIsInstance(elementType.javaObjectType)?.toList() ?: emptyList()
    }

    public fun getLocalDate(columnLabel: String): LocalDate {
        val localDate = getLocalDateOrNull(columnLabel)
        requireNotNull(localDate) { "Null-value for column label $columnLabel." }
        return localDate
    }

    public fun getLocalDateOrNull(columnLabel: String): LocalDate? {
        val date: Date? = resultSet.getDate(columnLabel)
        return date?.toLocalDate()
    }

    public fun getLocalDateTime(columnLabel: String): LocalDateTime {
        val localDateTime = getLocalDateTimeOrNull(columnLabel)
        requireNotNull(localDateTime) { "Null-value for column label $columnLabel." }
        return localDateTime
    }

    public fun getLocalDateTimeOrNull(columnLabel: String): LocalDateTime? {
        val timestamp: Timestamp? = resultSet.getTimestamp(columnLabel)
        return timestamp?.toLocalDateTime()
    }

    public fun getInstantOrNull(columnLabel: String): Instant? {
        val date: Timestamp? = resultSet.getTimestamp(columnLabel)
        return date?.toInstant()
    }

    public fun getInstant(columnLabel: String): Instant {
        return requireNotNull(getInstantOrNull(columnLabel)) { "Null-value for column label $columnLabel." }
    }

    public fun getPropertiesOrNull(columnLabel: String): Properties? {
        val dbData = resultSet.getString(columnLabel)
        return PropertiesParser.fromSql(dbData)
    }

    public fun getProperties(columnLabel: String): Properties {
        val propertiesOrNull = getPropertiesOrNull(columnLabel)
        requireNotNull(propertiesOrNull) { "Null-value for column label $columnLabel." }
        return propertiesOrNull
    }
}
