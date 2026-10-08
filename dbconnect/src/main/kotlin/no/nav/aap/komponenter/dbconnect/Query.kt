package no.nav.aap.komponenter.dbconnect

import java.sql.Connection
import java.sql.PreparedStatement

public class Query<T> internal constructor(
    private val preparedStatement: PreparedStatement,
    connection: Connection,
    namedIndexes: Map<String, List<Int>> = emptyMap()
) {
    private val params = Params(preparedStatement, connection, namedIndexes)
    private lateinit var rowMapper: (Row) -> T
    private var queryTimeout = 30

    private var paramsSet = false
    private fun assertParams() {
        require(!paramsSet) { "Kan ikke sette parametre flere ganger" }
        paramsSet = true
    }

    public fun setParams(block: Params.() -> Unit) {
        assertParams()
        params.block()
    }

    public fun setRowMapper(block: (Row) -> T) {
        rowMapper = block
    }

    public fun setQueryTimeout(sekunder: Int) {
        validering(sekunder)
        queryTimeout = sekunder
    }

    internal fun executeQuery(): Sequence<T> {
        params.validateNamedParameters()
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
