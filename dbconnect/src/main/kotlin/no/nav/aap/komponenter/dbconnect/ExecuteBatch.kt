package no.nav.aap.komponenter.dbconnect

import java.sql.PreparedStatement

class ExecuteBatch<out T>(private val preparedStatement: PreparedStatement, private val elements: Iterable<T>) {
    fun setParams(block: Params.(T) -> Unit) {
        elements.forEach { element ->
            Params(preparedStatement).block(element)
            preparedStatement.addBatch()
        }
    }

    internal fun execute() {
        preparedStatement.executeBatch()
    }
}