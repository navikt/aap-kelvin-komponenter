package no.nav.aap.komponenter.dbconnect

internal class Queryparser(private val query: String) {
    private companion object {
        private val REGEX = "(?:(\\?|:([a-zA-ZæøåÆØÅ0-9_]*))(?:::[^,]+)?)+".toRegex()
    }

    private val namedIndices = getNameIndices()
    private val isIndexed = group1Value().any("?"::equals)
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
        return group1()
            .asIterable()
            .reversed()
            .fold(query) { acc, group ->
                acc.replaceRange(group.range, "?")
            }
    }

    internal fun getIndices(name: String): List<Int>? {
        return namedIndices[name]
    }

    private fun getNameIndices(): Map<String, List<Int>> {
        return group2Value()
            .mapIndexed { index, name -> name to index + 1 }
            .groupBy(Pair<String, Int>::first, Pair<String, Int>::second)
    }

    private fun group1(): Sequence<MatchGroup> {
        return groups().mapNotNull { it[1] }
    }

    private fun group1Value(): Sequence<String> {
        return group1().map(MatchGroup::value)
    }

    private fun group2(): Sequence<MatchGroup> {
        return groups().mapNotNull { it[2] }
    }

    private fun group2Value(): Sequence<String> {
        return group2().map(MatchGroup::value)
    }

    private fun groups(): Sequence<MatchGroupCollection> {
        return generateSequence(REGEX.find(query), MatchResult::next).map(MatchResult::groups)
    }
}
