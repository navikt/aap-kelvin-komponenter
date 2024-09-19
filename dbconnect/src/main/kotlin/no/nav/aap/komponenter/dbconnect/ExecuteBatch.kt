package no.nav.aap.komponenter.dbconnect

import java.sql.PreparedStatement

public class ExecuteBatch<out T> internal constructor(
    private val preparedStatement: PreparedStatement,
    private val elements: Iterable<T>
) {
    public fun setParams(block: Params.(T) -> Unit) {
        elements.forEach { element ->
            Params(preparedStatement).block(element)
            preparedStatement.addBatch()
        }
    }

    internal fun execute() {
        preparedStatement.executeBatch()
    }
}
