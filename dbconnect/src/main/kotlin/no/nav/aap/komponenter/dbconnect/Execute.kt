package no.nav.aap.komponenter.dbconnect

import java.sql.Connection
import java.sql.PreparedStatement

public class Execute internal constructor(
    private val preparedStatement: PreparedStatement,
    private val connection: Connection
) {
    private var resultValidator: (Int) -> Unit = {}

    public fun setParams(block: Params.() -> Unit) {
        Params(preparedStatement, connection).block()
    }

    public fun setResultValidator(block: (Int) -> Unit) {
        resultValidator = block
    }

    internal fun execute() {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
    }

    internal fun executeReturnKey(): Long {
        return executeReturnKeysPrivate().single()
    }

    internal fun executeReturnKeys(): List<Long> {
        return executeReturnKeysPrivate().toList()
    }

    private fun executeReturnKeysPrivate(): Sequence<Long> {
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
        return preparedStatement
            .generatedKeys
            .map { it.getLong(1) }
    }
}
