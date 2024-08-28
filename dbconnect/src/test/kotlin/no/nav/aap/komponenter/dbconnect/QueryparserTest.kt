package no.nav.aap.komponenter.dbconnect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class QueryparserTest {

    @Test
    fun `Parse riktig med named parameters og daterange`() {
        val query = "SELECT * FROM TEST WHERE id = :id, range = :range::daterange, bil = :bil, annet = :range, lang_buss = :lang_buss"
        val queryIndexed = "SELECT * FROM TEST WHERE id = ?, range = ?::daterange, bil = ?, annet = ?, lang_buss = ?"
        val queryparser = Queryparser(query)
        assertThat(queryparser.getIndices("id")).containsExactly(1)
        assertThat(queryparser.getIndices("range")).containsExactly(2, 4)
        assertThat(queryparser.getIndices("bil")).containsExactly(3)
        assertThat(queryparser.getIndices("lang_buss")).containsExactly(5)
        assertThat(queryparser.getPreparedQuery()).isEqualTo(queryIndexed)
    }

    @Test
    fun `Parse riktig med indekserte parametre daterange`() {
        val query = "SELECT * FROM TEST WHERE id = ?, range = ?::daterange, bil = ?"
        val queryparser = Queryparser(query)
        assertThat(queryparser.getIndices("daterange")).isNull()
        assertThat(queryparser.getPreparedQuery()).isEqualTo(query)
    }

    @Test
    fun `Kan ikke blande named og indexed`() {
        assertThrows<IllegalArgumentException> {
            Queryparser("SELECT * FROM TEST WHERE id = :test, range = ?, bil = ?")
        }
        assertThrows<IllegalArgumentException> {
            Queryparser("SELECT * FROM TEST WHERE id = :test, range = ?::daterange, bil = ?")
        }
        assertThrows<IllegalArgumentException> {
            Queryparser("SELECT * FROM TEST WHERE id = :test::daterange, range = ?, bil = ?")
        }
    }

    @Test
    fun `Kan ha ingen parametre`() {
        assertDoesNotThrow {
            Queryparser("SELECT * FROM TEST")
        }
    }
}