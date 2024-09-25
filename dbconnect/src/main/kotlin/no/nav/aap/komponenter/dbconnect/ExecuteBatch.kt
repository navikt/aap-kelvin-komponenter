package no.nav.aap.komponenter.dbconnect

import java.sql.Connection
import java.sql.PreparedStatement

public class ExecuteBatch<out T> internal constructor(
    private val preparedStatement: PreparedStatement,
    private val connection: Connection,
    private val elements: Iterable<T>
) {
    public fun setParams(block: Params.(T) -> Unit) {
        elements.forEach { element ->
            Params(preparedStatement, connection).block(element)
            preparedStatement.addBatch()
        }
    }

    internal fun execute() {
        preparedStatement.executeBatch()
    }
}
