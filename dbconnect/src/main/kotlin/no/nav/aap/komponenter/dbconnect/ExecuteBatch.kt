package no.nav.aap.komponenter.dbconnect

import java.sql.Connection
import java.sql.PreparedStatement

public class ExecuteBatch<out T> internal constructor(
    private val preparedStatement: PreparedStatement,
    private val connection: Connection,
    private val elements: Iterable<T>,
    private val namedIndexes: Map<String, List<Int>> = emptyMap()
) {
    private var paramsSet = false

    public fun setParams(block: Params.(T) -> Unit) {
        elements.forEach { element ->
            if (namedIndexes.isNotEmpty()) preparedStatement.clearParameters()
            val params = Params(preparedStatement, connection, namedIndexes)
            params.block(element)
            params.validateNamedParameters()
            preparedStatement.addBatch()
        }
        paramsSet = true
    }

    internal fun execute() {
        require(namedIndexes.isEmpty() || paramsSet) {
            "Missing SQL parameters: ${namedIndexes.keys.joinToString()}"
        }
        preparedStatement.executeBatch()
    }
}
