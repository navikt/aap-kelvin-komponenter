package no.nav.aap.komponenter.dbconnect

import java.sql.Connection
import java.sql.PreparedStatement

public class Execute internal constructor(
    private val preparedStatement: PreparedStatement,
    connection: Connection,
    namedIndexes: Map<String, List<Int>> = emptyMap()
) {
    private var resultValidator: (Int) -> Unit = {}
    private val params = Params(preparedStatement, connection, namedIndexes)

    public fun setParams(block: Params.() -> Unit) {
        params.block()
    }

    public fun setResultValidator(block: (Int) -> Unit) {
        resultValidator = block
    }

    internal fun execute() {
        params.validateNamedParameters()
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
    }

    internal fun executeReturnUpdated(): Int {
        params.validateNamedParameters()
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
        return rowsUpdated
    }

    internal fun executeReturnKey(): Long {
        return executeReturnKeysPrivate().single()
    }

    internal fun executeReturnKeys(): List<Long> {
        return executeReturnKeysPrivate().toList()
    }

    private fun executeReturnKeysPrivate(): Sequence<Long> {
        params.validateNamedParameters()
        val rowsUpdated = preparedStatement.executeUpdate()
        resultValidator(rowsUpdated)
        return preparedStatement
            .generatedKeys
            .map { it.getLong(1) }
    }
}
