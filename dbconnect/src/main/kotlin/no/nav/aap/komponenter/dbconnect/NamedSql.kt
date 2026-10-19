package no.nav.aap.komponenter.dbconnect

internal data class NamedSql(
    val sql: String,
    val indexes: Map<String, List<Int>>
) {
    companion object {
        fun parse(sql: String): NamedSql {
            if (':' !in sql) return NamedSql(sql, emptyMap())
            val lexer = SqlLexer(sql)
            val context = SqlContext()
            val result = StringBuilder()
            val indexes = linkedMapOf<String, MutableList<Int>>()
            var parameterCount = 0
            var positional = false

            while (lexer.hasNext) {
                val token = lexer.nextToken()
                if (token.kind == TokenKind.IGNORED) {
                    result.append(token.text)
                    continue
                }

                val named = token.kind == TokenKind.NAMED && context.allowsNamedParameter
                val replacement = if (named) "?" else token.text
                when {
                    named -> indexes.getOrPut(token.text.drop(1)) { mutableListOf() }.add(++parameterCount)
                    token.kind == TokenKind.POSITIONAL -> {
                        positional = true
                        parameterCount++
                    }
                }
                result.append(replacement)
                context.update(
                    if (token.kind == TokenKind.NAMED && !named) token.text.drop(1) else replacement
                )
            }
            require(indexes.isEmpty() || !positional) {
                "Cannot mix named and positional SQL parameters"
            }
            return NamedSql(if (indexes.isEmpty()) sql else result.toString(), indexes)
        }
    }
}

private enum class TokenKind { IGNORED, TEXT, NAMED, POSITIONAL }

private data class SqlToken(val text: String, val kind: TokenKind)

private class SqlContext {
    private var parentheses = 0
    private val brackets = mutableListOf<Int?>()
    private var lastToken = ""

    // I subscripts er ':' et utsnitt; parenteser skiller ut navngitte parametre.
    val allowsNamedParameter: Boolean
        get() = brackets.isEmpty() || brackets.last() != parentheses

    fun update(token: String) {
        when (token) {
            "(" -> parentheses++
            ")" -> parentheses--
            "[" -> brackets.add(if (isArrayConstructor()) null else parentheses)
            "]" -> if (brackets.isNotEmpty()) brackets.removeAt(brackets.lastIndex)
        }
        lastToken = token
    }

    private fun isArrayConstructor(): Boolean =
        lastToken.equals("ARRAY", ignoreCase = true) || isNestedArrayConstructor()

    private fun isNestedArrayConstructor(): Boolean =
        brackets.isNotEmpty() && brackets.last() == null && (lastToken == "[" || lastToken == ",")
}

private class SqlLexer(private val sql: String) {
    private var position = 0

    val hasNext: Boolean
        get() = position < sql.length

    fun nextToken(): SqlToken {
        val start = position
        val kind = when {
            sql[start].isWhitespace() -> {
                position = readWhile(start, Char::isWhitespace)
                TokenKind.IGNORED
            }
            sql.startsWith("--", start) -> {
                position = readWhile(start + 2) { it != '\n' && it != '\r' }
                TokenKind.IGNORED
            }
            sql.startsWith("/*", start) -> {
                position = readBlockComment(start)
                TokenKind.IGNORED
            }
            else -> readSignificantToken(start)
        }
        return SqlToken(sql.substring(start, position), kind)
    }

    private fun readSignificantToken(start: Int): TokenKind {
        val char = sql[start]
        position = when {
            char == '\'' || char == '"' -> readQuotedString(start)
            char == '$' && isTokenBoundary(start) -> readDollarQuotedString(start)
            sql.startsWith("::", start) || sql.startsWith(":=", start) ||
                sql.startsWith("??", start) -> start + 2
            startsNamedParameter(start) -> {
                position = readWhile(start + 2, Char::isNamePart)
                return TokenKind.NAMED
            }
            char.isIdentifierPart() -> readWhile(start + 1, Char::isIdentifierPart)
            else -> start + 1
        }
        return if (char == '?' && position == start + 1) TokenKind.POSITIONAL else TokenKind.TEXT
    }

    private fun startsNamedParameter(start: Int): Boolean =
        sql[start] == ':' && sql.getOrNull(start + 1)?.isNameStart() == true && isTokenBoundary(start)

    private fun isTokenBoundary(start: Int): Boolean =
        start == 0 || !sql[start - 1].isIdentifierPart()

    private fun readQuotedString(start: Int): Int {
        val quote = sql[start]
        val escapes = quote == '\'' && isEscapeString(start)
        var end = start + 1
        while (end < sql.length) {
            when {
                escapes && sql[end] == '\\' -> end = (end + 2).coerceAtMost(sql.length)
                sql[end] != quote -> end++
                sql.getOrNull(end + 1) == quote -> end += 2
                else -> return end + 1
            }
        }
        return end
    }

    private fun isEscapeString(start: Int): Boolean =
        start > 0 && sql[start - 1].lowercaseChar() == 'e' && isTokenBoundary(start - 1)

    private fun readDollarQuotedString(start: Int): Int {
        val tagStart = start + 1
        val tagEnd = if (sql.getOrNull(tagStart)?.isDollarTagStart() == true) {
            readWhile(tagStart + 1, Char::isDollarTagPart)
        } else {
            tagStart
        }
        if (sql.getOrNull(tagEnd) != '$') return start + 1
        val delimiter = sql.substring(start, tagEnd + 1)
        val closing = sql.indexOf(delimiter, tagEnd + 1)
        return if (closing < 0) sql.length else closing + delimiter.length
    }

    private fun readBlockComment(start: Int): Int {
        var end = start + 2
        var depth = 1
        while (end < sql.length && depth > 0) {
            when {
                sql.startsWith("/*", end) -> {
                    depth++
                    end += 2
                }
                sql.startsWith("*/", end) -> {
                    depth--
                    end += 2
                }
                else -> end++
            }
        }
        return end
    }

    private fun readWhile(start: Int, matches: (Char) -> Boolean): Int {
        var end = start
        while (end < sql.length && matches(sql[end])) end++
        return end
    }
}

private fun Char.isNameStart(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
private fun Char.isNamePart(): Boolean = isNameStart() || this in '0'..'9'
private fun Char.isDollarTagStart(): Boolean = isNameStart() || this >= '\u0080'
private fun Char.isDollarTagPart(): Boolean = isDollarTagStart() || this in '0'..'9'
private fun Char.isIdentifierPart(): Boolean = isDollarTagPart() || this == '$'
