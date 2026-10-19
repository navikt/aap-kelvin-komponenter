package no.nav.aap.komponenter.dbconnect

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class NamedSqlTest {
    @Test
    fun `maps repeated case sensitive names in SQL order`() {
        val parsed = NamedSql.parse("SELECT :second, :first, :second, :Second, :_id2")
        assertThat(parsed.sql).isEqualTo("SELECT ?, ?, ?, ?, ?")
        assertThat(parsed.indexes).isEqualTo(
            mapOf("second" to listOf(1, 3), "first" to listOf(2), "Second" to listOf(4), "_id2" to listOf(5))
        )
    }

    @Test
    fun `preserves PostgreSQL literals identifiers comments and operators`() {
        val fragments = listOf(
            "':ignored ?'",
            "'it''s :ignored ?'",
            "\"column\"\":ignored ?\"",
            """E'it\'s :ignored ?'""",
            """e'backslash\\ :ignored ?'""",
            """'backslash\'""",
            "\$\$:ignored ? ' \"\$\$",
            "\$tag\$:ignored ? \$other\$ '\$tag\$",
            "\$\u00e6\$:ignored ?\$\u00e6\$",
            "/* :ignored ? /* nested :ignored */ still :ignored */",
            "-- :ignored ?\n",
            "-- :ignored ?\r",
            "value::text",
            "value := 1",
            "document ?? 'key'",
            "document ??| array['key']",
            "document ??& array['key']",
            "array[1:2]",
            "array[lower:upper]",
            "value[:upper]",
            "value[lower :upper]",
            "value[lower:upper][:next]",
            "identifier\$tag\$"
        )
        fragments.forEach { fragment ->
            val parsed = NamedSql.parse("SELECT $fragment, :actual::text")
            assertThat(parsed.sql).describedAs(fragment).isEqualTo("SELECT $fragment, ?::text")
            assertThat(parsed.indexes).describedAs(fragment).isEqualTo(mapOf("actual" to listOf(1)))
        }
    }

    @Test
    fun `leaves positional and parameterless SQL unchanged`() {
        listOf(
            "SELECT ?::text, ?",
            "SELECT ':literal', \"colon:name\", 1",
            "SELECT document ?? 'key'",
            "DO \$body\$ BEGIN value := ':name'; END \$body\$",
            "SELECT 1 -- :ignored"
        ).forEach { sql ->
            val parsed = NamedSql.parse(sql)
            assertThat(parsed.sql).isEqualTo(sql)
            assertThat(parsed.indexes).isEmpty()
        }
    }

    @Test
    fun `rejects mixed placeholders regardless of order`() {
        listOf("SELECT ?, :id", "SELECT :id, ?", "SELECT :id, ???").forEach { sql ->
            assertThrows<IllegalArgumentException> { NamedSql.parse(sql) }
        }
    }

    @Test
    fun `supports names in array constructors and parenthesized subscripts`() {
        val parsed = NamedSql.parse("SELECT ARRAY /* comment */ [:value], values[(:index)], values[(:low):(:high)]")
        assertThat(parsed.sql).isEqualTo("SELECT ARRAY /* comment */ [?], values[(?)], values[(?):(?)]")
        assertThat(parsed.indexes).isEqualTo(
            mapOf("value" to listOf(1), "index" to listOf(2), "low" to listOf(3), "high" to listOf(4))
        )
        val nested = NamedSql.parse("SELECT ARRAY[[:first], [:second]]")
        assertThat(nested.sql).isEqualTo("SELECT ARRAY[[?], [?]]")
        assertThat(nested.indexes).isEqualTo(mapOf("first" to listOf(1), "second" to listOf(2)))
    }

    @Test
    fun `preserves unterminated literals and comments for PostgreSQL to validate`() {
        listOf(
            "'unfinished :ignored ?",
            "\"unfinished :ignored ?",
            "E'unfinished\\",
            "\$tag\$unfinished :ignored ?",
            "/* unfinished :ignored ? /* nested */"
        ).forEach { fragment ->
            val parsed = NamedSql.parse("SELECT :actual::text, $fragment")
            assertThat(parsed.sql).describedAs(fragment).isEqualTo("SELECT ?::text, $fragment")
            assertThat(parsed.indexes).isEqualTo(mapOf("actual" to listOf(1)))
        }
    }

    @Test
    fun `ignores whitespace and comments when tracking array context`() {
        val parsed = NamedSql.parse(
            "SELECT ARRAY -- comment\n /* nested /* comment */ */ [[:first], /* comment */ [:second]], :third"
        )
        assertThat(parsed.sql).isEqualTo(
            "SELECT ARRAY -- comment\n /* nested /* comment */ */ [[?], /* comment */ [?]], ?"
        )
        assertThat(parsed.indexes).isEqualTo(
            mapOf("first" to listOf(1), "second" to listOf(2), "third" to listOf(3))
        )
    }
}
