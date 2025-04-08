package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.TemporalUnitWithinOffset
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

internal class ParamsOgRowTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @BeforeEach
    fun setup() {
        dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST_BYTES, TEST_STRING, TEST_ENUM, TEST_INT, TEST_LONG, TEST_BIG_DECIMAL, TEST_UUID, TEST_BOOLEAN, TEST_DATERANGE, TEST_LOCALDATE, TEST_LOCALDATETIME, TEST_INSTANT")
        }
    }

    @Test
    fun `Skriver og leser ByteArray og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BYTES (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBytes(1, "test".toByteArray())
                    setBytes(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BYTES") {
                setRowMapper { row ->
                    assertThat(row.getBytesOrNull("TEST")).asString().isEqualTo("test")
                    assertThat(row.getBytes("TEST")).asString().isEqualTo("test")
                    assertThat(row.getBytesOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBytes("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser String og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_STRING (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setString(1, "test")
                    setString(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_STRING") {
                setRowMapper { row ->
                    assertThat(row.getStringOrNull("TEST")).isEqualTo("test")
                    assertThat(row.getString("TEST")).isEqualTo("test")
                    assertThat(row.getStringOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getString("TEST_NULL") }
                }
            }
        }
    }

    private enum class TestEnum {
        TEST
    }

    @Test
    fun `Skriver og leser Enum og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_ENUM (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setEnumName(1, TestEnum.TEST)
                    setEnumName(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_ENUM") {
                setRowMapper { row ->
                    assertThat(row.getEnumOrNull<TestEnum?, TestEnum>("TEST")).isEqualTo(TestEnum.TEST)
                    assertThat(row.getEnum<TestEnum>("TEST")).isEqualTo(TestEnum.TEST)
                    assertThat(row.getEnumOrNull<TestEnum?, TestEnum>("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getEnum<TestEnum>("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Int og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_INT (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setInt(1, 0)
                    setInt(2, 1)
                    setInt(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_INT") {
                setRowMapper { row ->
                    assertThat(row.getIntOrNull("TEST_0")).isEqualTo(0)
                    assertThat(row.getInt("TEST_0")).isEqualTo(0)
                    assertThat(row.getIntOrNull("TEST_1")).isEqualTo(1)
                    assertThat(row.getInt("TEST_1")).isEqualTo(1)
                    assertThat(row.getIntOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getInt("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Long og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LONG (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLong(1, 0)
                    setLong(2, 1)
                    setLong(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LONG") {
                setRowMapper { row ->
                    assertThat(row.getLongOrNull("TEST_0")).isEqualTo(0)
                    assertThat(row.getLong("TEST_0")).isEqualTo(0)
                    assertThat(row.getLongOrNull("TEST_1")).isEqualTo(1)
                    assertThat(row.getLong("TEST_1")).isEqualTo(1)
                    assertThat(row.getLongOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLong("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Double og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_DOUBLE (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setDouble(1, 0.0)
                    setDouble(2, 1.4)
                    setDouble(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_DOUBLE") {
                setRowMapper { row ->
                    assertThat(row.getDoubleOrNull("TEST_0")).isEqualTo(0.0)
                    assertThat(row.getDouble("TEST_0")).isEqualTo(0.0)
                    assertThat(row.getDoubleOrNull("TEST_1")).isEqualTo(1.4)
                    assertThat(row.getDouble("TEST_1")).isEqualTo(1.4)
                    assertThat(row.getDoubleOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLong("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser BigDecimal og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BIG_DECIMAL (TEST_0, TEST_1, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBigDecimal(1, BigDecimal.ZERO)
                    setBigDecimal(2, BigDecimal("12.34"))
                    setBigDecimal(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BIG_DECIMAL") {
                setRowMapper { row ->
                    // Er lik 0.00 fordi kolonnen i tabellen precision = 4 og scale = 2
                    assertThat(row.getBigDecimalOrNull("TEST_0")).isEqualTo("0.00")
                    assertThat(row.getBigDecimal("TEST_0")).isEqualTo("0.00")
                    assertThat(row.getBigDecimalOrNull("TEST_1")).isEqualTo(BigDecimal("12.34"))
                    assertThat(row.getBigDecimal("TEST_1")).isEqualTo(BigDecimal("12.34"))
                    assertThat(row.getBigDecimalOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBigDecimal("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser UUID og null-verdi riktig`() {
        val randomUUID = UUID.randomUUID()
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_UUID (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setUUID(1, randomUUID)
                    setUUID(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_UUID") {
                setRowMapper { row ->
                    assertThat(row.getUUIDOrNull("TEST")).isEqualTo(randomUUID)
                    assertThat(row.getUUID("TEST")).isEqualTo(randomUUID)
                    assertThat(row.getUUIDOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getUUID("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Boolean og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_BOOLEAN (TEST_FALSE, TEST_TRUE, TEST_NULL)
                    VALUES (?, ?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setBoolean(1, false)
                    setBoolean(2, true)
                    setBoolean(3, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_BOOLEAN") {
                setRowMapper { row ->
                    assertThat(row.getBooleanOrNull("TEST_FALSE")).isFalse
                    assertThat(row.getBoolean("TEST_FALSE")).isFalse
                    assertThat(row.getBooleanOrNull("TEST_TRUE")).isTrue
                    assertThat(row.getBoolean("TEST_TRUE")).isTrue
                    assertThat(row.getBooleanOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getBoolean("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Periode og null-verdi riktig`() {
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_DATERANGE (TEST, TEST_NULL)
                    VALUES (?::daterange, ?::daterange)
                """.trimMargin()
            ) {
                setParams {
                    setPeriode(1, Periode(LocalDate.now(), LocalDate.now()))
                    setPeriode(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_DATERANGE") {
                setRowMapper { row ->
                    assertThat(row.getPeriodeOrNull("TEST"))
                        .isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriode("TEST")).isEqualTo(Periode(LocalDate.now(), LocalDate.now()))
                    assertThat(row.getPeriodeOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getPeriode("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser LocalDate og null-verdi riktig`() {
        val localDate = LocalDate.of(2016, Month.AUGUST, 12)
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LOCALDATE (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLocalDate(1, localDate)
                    setLocalDate(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LOCALDATE") {
                setRowMapper { row ->
                    assertThat(row.getLocalDateOrNull("TEST")).isEqualTo(localDate)
                    assertThat(row.getLocalDate("TEST")).isEqualTo(localDate)
                    assertThat(row.getLocalDateOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLocalDate("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser LocalDateTime og null-verdi riktig`() {
        val localDateTime = LocalDateTime.of(2016, Month.AUGUST, 12, 9, 38, 12, 123456000)
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_LOCALDATETIME (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setLocalDateTime(1, localDateTime)
                    setLocalDateTime(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_LOCALDATETIME") {
                setRowMapper { row ->
                    assertThat(row.getLocalDateTimeOrNull("TEST")).isEqualTo(localDateTime)
                    assertThat(row.getLocalDateTime("TEST")).isEqualTo(localDateTime)
                    assertThat(row.getLocalDateTimeOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getLocalDateTime("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser Instant og null-verdi riktig`() {
        val instant = Instant.parse("2016-08-12T09:38:12Z")
        dataSource.transaction { connection ->
            connection.execute(
                """
                    INSERT INTO TEST_INSTANT (TEST, TEST_NULL)
                    VALUES (?, ?)
                """.trimMargin()
            ) {
                setParams {
                    setInstant(1, instant)
                    setInstant(2, null)
                }
            }
            connection.queryFirst("SELECT * FROM TEST_INSTANT") {
                setRowMapper { row ->
                    assertThat(row.getInstantOrNull("TEST")).isEqualTo(instant)
                    assertThat(row.getInstant("TEST")).isEqualTo(instant)
                    assertThat(row.getInstantOrNull("TEST_NULL")).isNull()
                    assertThrows<IllegalArgumentException> { row.getInstant("TEST_NULL") }
                }
            }
        }
    }

    @Test
    fun `Skriver og leser med listeparametre`() {
        dataSource.transaction { connection ->
            connection.executeBatch(
                """
                    INSERT INTO TEST_ARRAY_QUERY (TEST)
                    VALUES (?)
                """,
                listOf("1", "2", "3", "4")
            ) {
                setParams { string ->
                    setString(1, string)
                }
            }
            val strings = connection.queryList("SELECT * FROM TEST_ARRAY_QUERY WHERE TEST = ANY(?::text[])") {
                setParams {
                    setArray(1, listOf("2", "3"))
                }
                setRowMapper { row ->
                    row.getString("TEST")
                }
            }
            assertThat(strings).hasSize(2)
                .containsExactly("2", "3")
        }
    }

    @Test
    fun `Skriver og leser med periode-array`() {
        dataSource.transaction { connection ->
            val perioderInn = listOf(
                Periode(LocalDate.of(1990, 6, 1), LocalDate.of(2200, 1, 1)),
                Periode(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 5, 3)),
            )
            connection.execute(
                """
                    INSERT INTO TEST_PERIODE_ARRAY(TEST)
                    VALUES (?::daterange[])
                """,
            ) {
                setParams {
                    setPeriodeArray(1, perioderInn)
                }
            }


            val perioderUt = connection.queryFirst("SELECT TEST FROM TEST_PERIODE_ARRAY") {
                setRowMapper { row ->
                    row.getPeriodeArray("TEST")
                }
            }
            assertThat(perioderUt)
                .isEqualTo(perioderInn)
        }
    }

    @Test
    fun `CURRENT_TIMESTAMP i postgres (UTC) matcher LocalDateTime now() i pod (Europe Oslo)`() {
        dataSource.transaction { connection ->
            val currentTimestamp = connection.queryFirst("""
                SELECT CURRENT_TIMESTAMP as LOCALDATETIME;
            """.trimIndent()
            ) {
                setRowMapper { row ->
                    row.getLocalDateTime("LOCALDATETIME")
                }
            }

            assertThat(currentTimestamp).isCloseTo(
                LocalDateTime.now(ZoneId.of("Europe/Oslo")),
                TemporalUnitWithinOffset(1, ChronoUnit.MINUTES)
            )
        }
    }

    @Test
    fun `CURRENT_TIMESTAMP i postgres (UTC) matcher Instant now() i pod (Europe Oslo)`() {
        dataSource.transaction { connection ->
            val currentTimestamp = connection.queryFirst("""
                SELECT CURRENT_TIMESTAMP as INSTANT;
            """.trimIndent()
            ) {
                setRowMapper { row ->
                    row.getInstant("INSTANT")
                }
            }

            assertThat(currentTimestamp).isCloseTo(
                Instant.now(),
                TemporalUnitWithinOffset(1, ChronoUnit.MINUTES)
            )
        }
    }

    @Test
    fun `get array of integers`() {
        dataSource.transaction { connection ->
            val array = connection.queryFirst("""
                SELECT ARRAY[1, 2, 3] AS my_array;
            """.trimIndent()
            ) {
                setRowMapper { row ->
                    row.getArray("my_array", Int::class)
                }
            }

            assertThat(array).containsExactly(1, 2, 3)
        }
    }
    @Test
    fun `get array of strings`() {
        dataSource.transaction { connection ->
            val array = connection.queryFirst("""
                SELECT ARRAY['ff', 'hei', 'df'] AS my_array;
            """.trimIndent()
            ) {
                setRowMapper { row ->
                    row.getArray("my_array", String::class)
                }
            }

            assertThat(array).containsExactly("ff", "hei", "df")
        }
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            /** [java.sql.Timestamp] bruker default timezone for å konvertere til og fra
             * [java.time.LocalDateTime]. I pod-ene og på laptopene våre kjører vi med
             * Europe/Oslo mens github action kjører med UTC.
             *
             * Er litt stygt å endre timezone mens programmet kjører. I stede for å sette
             * timezone her, kan vi sette timezone i Github Actions, man da vil ikke unit-tester
             * kunne kjøre på en random maskin uten at det konfigureres først.
             */
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Oslo"))
        }
    }
}
