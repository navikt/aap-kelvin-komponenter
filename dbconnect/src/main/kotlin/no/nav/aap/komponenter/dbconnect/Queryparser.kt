package no.nav.aap.komponenter.dbconnect

internal class Queryparser(private val query: String) {
    private val namedIndices: Map<String, Set<Int>>
    private val preparedQuery: String

    init {
        val (preparedQuery, indices) = findNames(query)
        this.namedIndices = indices.toMap()
        this.preparedQuery = preparedQuery
    }

    private val isIndexed = query.contains('?')
    private val isNamed = namedIndices.isNotEmpty()

    init {
        require(!isIndexed || !isNamed) { "Kan ikke bruke både indexed og named parametre " }
    }


    internal fun isIndexed(): Boolean {
        return isIndexed
    }

    internal fun isNamed(): Boolean {
        return isNamed
    }

    internal fun getPreparedQuery(): String {
        if (isIndexed) {
            return query
        }
        return preparedQuery
    }

    internal fun getIndices(name: String): Set<Int>? {
        return namedIndices[name]
    }

    private companion object {
        private fun findNames(query: String, index: Int = 1): Pair<String, Map<String, Set<Int>>> {
            if (query.isEmpty()) {
                return query to emptyMap()
            }

            val i = query.indexOf(':')
            if (query.lastIndex == i) {
                //Siste tegn i query. Uansett ikke en parameter
                return query to emptyMap()
            }
            if (i == -1) {
                //Finner ingen flere parametre
                return query to emptyMap()
            }

            val førsteDel = query.substring(0, i)
            val sisteDel = query.substring(i)

            if (sisteDel.length <= 1) {
                //Delen fra og med index er for kort til å være et parameter
                //Denne vil aldri være sann pga `if (query.lastIndex == i)`
                return query to emptyMap()
            }

            val navn = sisteDel.drop(1).takeWhile(Char::gyldig)
            val resten = sisteDel.drop(2).dropWhile(Char::gyldig)

            if (navn.isEmpty()) {
                val erstattning = sisteDel.take(2) + sisteDel.drop(2).takeWhile(Char::gyldig)
                val (resQuery, mappet) = findNames(resten, index)
                return (erstattning + resQuery) to mappet
            }

            val (resQuery, mappet) = findNames(resten, index + 1)
            val nyDelquery = "$førsteDel?$resQuery"
            val nyttMap = mappet.toMutableMap()
            nyttMap.merge(navn, setOf(index)) { ints, elements -> elements.plus(ints) }

            return nyDelquery to nyttMap.toMap()
        }
    }
}

private fun Char.gyldig(): Boolean {
    return isLetter() || this in "_"
}
