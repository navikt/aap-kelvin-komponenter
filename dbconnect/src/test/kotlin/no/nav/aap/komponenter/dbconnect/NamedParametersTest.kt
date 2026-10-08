package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Tidspunkt
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

internal class NamedParametersTest {
    @AutoClose
    private val dataSource = TestDataSource()

    private enum class Example { VALUE }

    @Test
    fun `supports every typed setter and repeated names`() {
        val date = LocalDate.of(2026, 1, 2)
        val dateTime = LocalDateTime.of(2026, 1, 2, 12, 30)
        val instant = Instant.parse("2026-01-02T12:30:00Z")
        val tidspunkt = Tidspunkt.parse("2026-01-02T12:30:00Z")
        val periode = Periode(date, date.plusDays(2))
        val uuid = UUID.randomUUID()
        val properties = Properties().apply { setProperty("key", "value") }
        dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst(
                """
                SELECT :bytes::bytea AS bytes, :string::text AS string, :enum::text AS enum,
                       :int::int AS int, :long::bigint AS long, :double::float8 AS double,
                       :decimal::numeric AS decimal, :uuid::uuid AS uuid, :boolean::boolean AS boolean,
                       :periode::daterange AS periode, :date::date AS date, :datetime::timestamp AS datetime,
                       :instant::timestamp AS instant, :tidspunkt::timestamp AS tidspunkt,
                       :properties::text AS properties, :bruker::text AS bruker,
                       :strings::text[] AS strings, :longs::bigint[] AS longs,
                       :perioder::daterange[] AS perioder, :uuids::uuid[] AS uuids,
                       :string::text AS repeated, :strings::text[] AS repeated_array
                """.trimIndent()
            ) {
                setParams {
                    setUUIDArray("uuids", listOf(uuid))
                    setPeriodeArray("perioder", listOf(periode))
                    setLongArray("longs", listOf(1L, 2L))
                    setArray("strings", listOf("a", "b"))
                    setBruker("bruker", Bruker("Z00000"))
                    setProperties("properties", properties)
                    setTidspunkt("tidspunkt", tidspunkt)
                    setInstant("instant", instant)
                    setLocalDateTime("datetime", dateTime)
                    setLocalDate("date", date)
                    setPeriode("periode", periode)
                    setBoolean("boolean", true)
                    setUUID("uuid", uuid)
                    setBigDecimal("decimal", BigDecimal("12.34"))
                    setDouble("double", 12.5)
                    setLong("long", 123L)
                    setInt("int", 42)
                    setEnumName("enum", Example.VALUE)
                    setString("string", "value'; DROP TABLE test; --")
                    setBytes("bytes", byteArrayOf(1, 2))
                }
                setRowMapper { row ->
                    assertThat(row.getBytes("bytes")).containsExactly(1, 2)
                    assertThat(row.getString("string")).isEqualTo("value'; DROP TABLE test; --")
                    assertThat(row.getString("repeated")).isEqualTo(row.getString("string"))
                    assertThat(row.getEnum<Example>("enum")).isEqualTo(Example.VALUE)
                    assertThat(row.getInt("int")).isEqualTo(42)
                    assertThat(row.getLong("long")).isEqualTo(123L)
                    assertThat(row.getDouble("double")).isEqualTo(12.5)
                    assertThat(row.getBigDecimal("decimal")).isEqualTo(BigDecimal("12.34"))
                    assertThat(row.getUUID("uuid")).isEqualTo(uuid)
                    assertThat(row.getBoolean("boolean")).isTrue()
                    assertThat(row.getPeriode("periode")).isEqualTo(periode)
                    assertThat(row.getLocalDate("date")).isEqualTo(date)
                    assertThat(row.getLocalDateTime("datetime")).isEqualTo(dateTime)
                    assertThat(row.getInstant("instant")).isEqualTo(instant)
                    assertThat(row.getTidspunkt("tidspunkt")).isEqualTo(tidspunkt)
                    assertThat(row.getString("properties")).isEqualTo(PropertiesParser.toSql(properties))
                    assertThat(row.getBruker("bruker")).isEqualTo(Bruker("Z00000"))
                    assertThat(row.getArray("strings", String::class)).containsExactly("a", "b")
                    assertThat(row.getArray("repeated_array", String::class)).containsExactly("a", "b")
                    assertThat(row.getArray("longs", Long::class)).containsExactly(1L, 2L)
                    assertThat(row.getPeriodeArray("perioder")).containsExactly(periode)
                    assertThat(row.getArray("uuids", UUID::class)).containsExactly(uuid)
                }
            }
        }
    }

    @Test
    fun `supports null for every nullable setter`() {
        val types = linkedMapOf(
            "bytes" to "bytea", "string" to "text", "enum" to "text", "int" to "int",
            "long" to "bigint", "double" to "float8", "decimal" to "numeric", "uuid" to "uuid",
            "boolean" to "boolean", "periode" to "daterange", "date" to "date",
            "datetime" to "timestamp", "instant" to "timestamp", "tidspunkt" to "timestamp",
            "properties" to "text", "bruker" to "text", "perioder" to "daterange[]"
        )
        val sql = "SELECT " + types.entries.joinToString { (name, type) -> ":$name::$type AS $name" }
        dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst(sql) {
                setParams {
                    setBytes("bytes", null)
                    setString("string", null)
                    setEnumName("enum", null)
                    setInt("int", null)
                    setLong("long", null)
                    setDouble("double", null)
                    setBigDecimal("decimal", null)
                    setUUID("uuid", null)
                    setBoolean("boolean", null)
                    setPeriode("periode", null)
                    setLocalDate("date", null)
                    setLocalDateTime("datetime", null)
                    setInstant("instant", null)
                    setTidspunkt("tidspunkt", null)
                    setProperties("properties", null)
                    setBruker("bruker", null)
                    setPeriodeArray("perioder", null)
                }
                setRowMapper { row ->
                    types.keys.forEach { name -> assertThat(row.getStringOrNull(name)).describedAs(name).isNull() }
                }
            }
        }
    }

    @Test
    fun `supports writes generated keys and all query variants`() {
        dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES (:value)") {
                setParams { setString("value", "a") }
                setResultValidator { assertThat(it).isEqualTo(1) }
            }
            val key = connection.executeReturnKey("INSERT INTO test (test) VALUES (:value)") {
                setParams { setString("value", "b") }
            }
            val keys = connection.executeReturnKeys("INSERT INTO test (test) VALUES (:first), (:second)") {
                setParams {
                    setString("second", "d")
                    setString("first", "c")
                }
            }
            assertThat(key).isEqualTo(2L)
            assertThat(keys).containsExactly(3L, 4L)
            val updated = connection.executeReturnUpdated("UPDATE test SET test = :value WHERE id = :id") {
                setParams {
                    setLong("id", key)
                    setString("value", "updated")
                }
            }
            assertThat(updated).isEqualTo(1)
            assertThat(connection.queryFirst<String>("SELECT test FROM test WHERE id = :id") {
                setParams { setLong("id", key) }
                setRowMapper { it.getString("test") }
            }).isEqualTo("updated")
            assertThat(connection.queryFirstOrNull<String>("SELECT test FROM test WHERE id = :id") {
                setParams { setLong("id", -1L) }
                setRowMapper { it.getString("test") }
            }).isNull()
            assertThat(connection.queryList("SELECT test FROM test WHERE id = ANY(:ids::bigint[]) ORDER BY id") {
                setParams { setLongArray("ids", keys) }
                setRowMapper { it.getString("test") }
            }).containsExactly("c", "d")
            assertThat(connection.querySet("SELECT test FROM test WHERE id = ANY(:ids::bigint[])") {
                setParams { setLongArray("ids", keys) }
                setRowMapper { it.getString("test") }
            }).containsExactlyInAnyOrder("c", "d")
        }
    }

    @Test
    fun `supports batches across chunk boundaries and explicit nulls`() {
        val elements = (1..8001).map { if (it % 2 == 0) null else "value$it" }
        dataSource.transaction { connection ->
            connection.executeBatch("INSERT INTO test (test) VALUES (:value)", elements) {
                setParams { element -> setString("value", element) }
            }
            assertThat(connection.queryList("SELECT test, id FROM test ORDER BY id") {
                setRowMapper { it.getStringOrNull("test") to it.getLong("id") }
            }.map { it.first }).containsExactlyElementsOf(elements)
        }
    }

    @Test
    fun `rejects missing unknown and mixed parameters`() {
        dataSource.transaction { connection ->
            assertThat(assertThrows<IllegalArgumentException> {
                connection.queryFirst("SELECT :value::text") {
                    setRowMapper { it.getString("value") }
                }
            }).hasMessageContaining("Missing SQL parameters: value")
            assertThat(assertThrows<IllegalArgumentException> {
                connection.execute("INSERT INTO test (test) VALUES (:value)") {
                    setParams { setString("typo", "secret") }
                }
            }).hasMessageContaining("Unknown SQL parameter: typo").hasMessageNotContaining("secret")
            assertThrows<IllegalArgumentException> {
                connection.execute("INSERT INTO test (test) VALUES (:value)")
            }
            assertThat(assertThrows<IllegalArgumentException> {
                connection.queryFirst("SELECT :first::text AS first, :second::text AS second") {
                    setParams { setString("first", "a") }
                    setRowMapper { it.getString("first") }
                }
            }).hasMessage("Missing SQL parameters: second")
            assertThrows<IllegalArgumentException> {
                connection.execute("INSERT INTO test (test) VALUES (:value), (?)")
            }
            assertThrows<IllegalArgumentException> {
                connection.executeReturnKey("INSERT INTO test (test) VALUES (:value)")
            }
            assertThrows<IllegalArgumentException> {
                connection.executeReturnKeys("INSERT INTO test (test) VALUES (:value)")
            }
            assertThrows<IllegalArgumentException> {
                connection.executeReturnUpdated("INSERT INTO test (test) VALUES (:value)")
            }
            assertThrows<IllegalArgumentException> {
                connection.executeBatch("INSERT INTO test (test) VALUES (:value)", listOf("a"))
            }
        }
    }

    @Test
    fun `missing batch binding cannot reuse a previous element value`() {
        dataSource.transaction { connection ->
            assertThat(assertThrows<IllegalArgumentException> {
                connection.executeBatch("INSERT INTO test (test) VALUES (:value)", listOf("a", "b")) {
                    setParams { element -> if (element == "a") setString("value", element) }
                }
            }).hasMessageContaining("Missing SQL parameters: value")
            assertThat(connection.queryFirst<Long>("SELECT count(*) AS count FROM test") {
                setRowMapper { it.getLong("count") }
            }).isZero()
        }
    }

    @Test
    fun `preserves casts JSON operators dollar quoting and comments against PostgreSQL`() {
        dataSource.transaction(readOnly = true) { connection ->
            val result = connection.queryFirst(
                """
                SELECT :value::text = ${'$'}tag${'$'}:literal ?${'$'}tag${'$'} AS matches,
                       '{"key": 1}'::jsonb ?? :key AS has_key,
                       E'it\'s :literal ?' AS escaped,
                       ':literal' AS ":identifier"
                /* :ignored /* :nested */ ? */ -- :ignored ?
                """.trimIndent()
            ) {
                setParams {
                    setString("key", "key")
                    setString("value", ":literal ?")
                }
                setRowMapper { row ->
                    assertThat(row.getString("escaped")).isEqualTo("it's :literal ?")
                    assertThat(row.getString(":identifier")).isEqualTo(":literal")
                    row.getBoolean("matches") && row.getBoolean("has_key")
                }
            }
            assertThat(result).isTrue()
        }
    }

    @Test
    fun `preserves native array slices and supports named subscripts and constructors`() {
        dataSource.transaction(readOnly = true) { connection ->
            connection.queryFirst(
                """
                SELECT items[:upper] AS slice, items[(:index)] AS item,
                       items[(:low):(:high)] AS named_slice, ARRAY[:value] AS constructed
                FROM (SELECT ARRAY[10, 20, 30] AS items, 2 AS upper) AS source
                """.trimIndent()
            ) {
                setParams {
                    setInt("index", 3)
                    setInt("low", 2)
                    setInt("high", 3)
                    setInt("value", 40)
                }
                setRowMapper { row ->
                    assertThat(row.getArray("slice", Int::class)).containsExactly(10, 20)
                    assertThat(row.getInt("item")).isEqualTo(30)
                    assertThat(row.getArray("named_slice", Int::class)).containsExactly(20, 30)
                    assertThat(row.getArray("constructed", Int::class)).containsExactly(40)
                }
            }
        }
    }
}
