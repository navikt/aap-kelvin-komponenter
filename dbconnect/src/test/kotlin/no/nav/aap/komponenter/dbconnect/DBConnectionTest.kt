package no.nav.aap.komponenter.dbconnect

import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DBConnectionTest {
    private val dataSource = InitTestDatabase.freshDatabase()

    @BeforeEach
    fun setup() {
        dataSource.transaction { connection ->
            connection.execute("TRUNCATE TEST; ALTER SEQUENCE test_id_seq RESTART WITH 1")
        }
    }

    @Test
    fun `Skriver og henter en rad mot DB`() {
        val result = dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a')")
            connection.queryFirst("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isEqualTo("a")
    }


    @Test
    fun `Skriver og henter to rader mot DB`() {
        val result = dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a'), ('b')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result)
            .hasSize(2)
            .contains("a", "b")
    }

    @Test
    fun `Kan returnere antall rader oppdatert`() {
        val insert = dataSource.transaction { connection ->
            connection.executeReturnUpdated("INSERT INTO test (test) VALUES ('a'), ('b')")
        }
        val update = dataSource.transaction { connection ->
            connection.executeReturnUpdated("UPDATE test set test = 'c' where test = 'a'")
        }
        assertThat(insert).isEqualTo(2)
        assertThat(update).isEqualTo(1)
    }

    @Test
    fun `Henter ingen rader fra DB`() {
        val result = dataSource.transaction { connection ->
            connection.queryFirstOrNull("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isNull()
    }

    @Test
    fun `Henter null-verdi fra DB`() {
        val result = dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES (null)")
            connection.queryFirstOrNull("SELECT test FROM test") {
                setRowMapper { row -> row.getStringOrNull("test") }
            }
        }

        assertThat(result).isNull()
    }

    @Test
    fun `Skriver og henter key og verdier fra DB`() {
        val (result, key) = dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a')")
            val key = connection.executeReturnKey("INSERT INTO test (test) VALUES ('b')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            } to key
        }

        assertThat(result)
            .hasSize(2)
            .contains("a", "b")
        assertThat(key).isEqualTo(2)
    }

    @Test
    fun `Skriver og henter keys og verdier fra DB`() {
        val (result, keys) = dataSource.transaction { connection ->
            connection.execute("INSERT INTO test (test) VALUES ('a'), ('b')")
            val keys = connection.executeReturnKeys("INSERT INTO test (test) VALUES ('c'), ('d')")
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            } to keys
        }

        assertThat(result)
            .hasSize(4)
            .contains("a", "b", "c", "d")
        assertThat(keys)
            .hasSize(2)
            .contains(3, 4)
    }

    @Test
    fun `Henter tom liste fra DB`() {
        val result = dataSource.transaction { connection ->
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).isEmpty()
    }

    @Test
    fun `Skriver og henter batch mot DB`() {
        val elements = listOf("a", "b", "c")
        val result = dataSource.transaction { connection ->
            connection.executeBatch("INSERT INTO test (test) VALUES (?)", elements) {
                setParams { element ->
                    setString(1, element)
                }
            }
            connection.queryList("SELECT test FROM test") {
                setRowMapper { row -> row.getString("test") }
            }
        }

        assertThat(result).containsExactly("a", "b", "c")
    }
}
