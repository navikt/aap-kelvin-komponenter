package no.nav.aap.komponenter.dbconnect

import java.sql.PreparedStatement

public class Query<T> internal constructor(
    private val preparedStatement: PreparedStatement,
    private val queryparser: Queryparser
) {
    private lateinit var rowMapper: (Row) -> T
    private var queryTimeout = 30

    private var paramsSet = false
    private fun assertParams() {
        require(!paramsSet) { "Kan ikke sette paramertre flere ganger" }
        paramsSet = true
    }

    public fun setParams(block: Params.() -> Unit) {
        assertParams()
        require(queryparser.isIndexed())
        Params(preparedStatement).block()
    }

    public fun setParamsAutoIndex(block: ParamsAutoIndex.() -> Unit) {
        assertParams()
        require(queryparser.isIndexed())
        ParamsAutoIndex(preparedStatement).block()
    }

    public fun setNamedParams(block: NamedParams.() -> Unit) {
        assertParams()
        require(queryparser.isNamed())
        NamedParams(preparedStatement, queryparser).block()
    }

    public fun setRowMapper(block: (Row) -> T) {
        rowMapper = block
    }

    public fun setQueryTimeout(sekunder: Int) {
        validering(sekunder)
        queryTimeout = sekunder
    }

    internal fun executeQuery(): Sequence<T> {
        val resultSet = preparedStatement.executeQuery()
        preparedStatement.queryTimeout = queryTimeout
        return resultSet
            .map { currentResultSet ->
                rowMapper(Row(currentResultSet))
            }
    }
}

internal fun validering(sekunder: Int) {
    require(sekunder in 300 downTo 1)
}
