package no.nav.aap.komponenter.dbconnect

import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.Savepoint
import java.sql.Statement

private const val QUERY_TIMEOUT_IN_SECONDS = 30

@Suppress("SqlSourceToSinkFlow")
public class DBConnection internal constructor(
    private val connection: Connection
) {
    private var savepoint: Savepoint? = null

    public fun execute(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ) {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            preparedStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
            val executeStatement = Execute(preparedStatement, this.connection)
            executeStatement.block()
            executeStatement.execute()
        }
    }

    public fun <T> executeBatch(
        @Language("PostgreSQL")
        query: String,
        elements: Iterable<T>,
        block: ExecuteBatch<T>.() -> Unit = {}
    ) {
        if (elements.none()) {
            return
        }
        elements.chunked(8000).forEach { subelement ->
            this.connection.prepareStatement(query).use { preparedStatement ->
                preparedStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
                val executeStatement = ExecuteBatch(preparedStatement, this.connection, subelement)
                executeStatement.block()
                executeStatement.execute()
            }
        }
    }

    /**
     * Executes the given SQL query and returns the generated key.
     *
     * @sample
     * val key = connection.executeReturnKey(query) {
     *     setParams {
     *         setLong(1, avklaringsbehovId)
     *         setEnumName(2, endring.status)
     *         setString(3, endring.begrunnelse)
     *         setLocalDate(4, endring.frist)
     *         setString(5, opprettetAv)
     *         setLocalDateTime(6, LocalDateTime.now())
     *         setEnumName(7, endring.grunn)
     *     }
     * }
     *
     * @param query The SQL query to execute.
     * @param block Lambda function to configure the execution parameters and result validation.
     * @return The generated key as a Long.
     */
    public fun executeReturnKey(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): Long {
        return this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
            preparedStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
            val executeStatement = Execute(preparedStatement, this.connection)
            executeStatement.block()
            return@use executeStatement.executeReturnKey()
        }
    }

    public fun executeReturnKeys(
        @Language("PostgreSQL")
        query: String,
        block: Execute.() -> Unit = {}
    ): List<Long> {
        return this.connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS).use { preparedStatement ->
            preparedStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
            val executeStatement = Execute(preparedStatement, this.connection)
            executeStatement.block()
            return@use executeStatement.executeReturnKeys()
        }
    }

    public fun <T> queryFirstOrNull(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): T? {
        return querySeq(query, block, Sequence<T>::firstOrNull)
    }

    /**
     * Executes a query and retrieves the first result.
     *
     * @param query The SQL query to execute
     * @param block The block of code to customize the query
     * @return The first result of the query.
     * @throws java.util.NoSuchElementException If there are zero results.
     */
    public fun <T : Any> queryFirst(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): T {
        return querySeq(query, block, Sequence<T>::first)
    }

    public fun <T : Any> queryList(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): List<T> {
        return querySeq(query, block, Sequence<T>::toList)
    }

    public fun <T : Any> querySet(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit
    ): Set<T> {
        return querySeq(query, block, Sequence<T>::toSet)
    }

    private fun <T, U> querySeq(
        @Language("PostgreSQL")
        query: String,
        block: Query<T>.() -> Unit,
        extractor: Sequence<T>.() -> U
    ): U {
        return this.connection.prepareStatement(query).use { preparedStatement ->
            preparedStatement.queryTimeout = QUERY_TIMEOUT_IN_SECONDS
            val queryStatement = Query<T>(preparedStatement, this.connection)
            queryStatement.block()
            val result = queryStatement.executeQuery()
            return@use result.extractor()
        }
    }

    public fun markerSavepoint() {
        savepoint = this.connection.setSavepoint()
    }

    internal fun rollback() {
        if (savepoint != null) {
            this.connection.rollback(savepoint)
        } else {
            this.connection.rollback()
        }
    }

    internal fun commit() {
        this.connection.commit()
    }

    internal fun autoCommitOn() {
        this.connection.autoCommit = true
    }

    internal fun autoCommitOff() {
        this.connection.autoCommit = false
    }
}
