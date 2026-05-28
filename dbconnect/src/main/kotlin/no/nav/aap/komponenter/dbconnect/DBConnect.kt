package no.nav.aap.komponenter.dbconnect

import javax.sql.DataSource


/**
 * Start en transaksjon.
 *
 * @param readOnly Om transaksjonen skal være i read-only modus. Defaulter til false.
 */
public fun <T> DataSource.transaction(readOnly: Boolean = false, block: (DBConnection) -> T): T {
    return span("transaction", listOf("readOnly" to readOnly.toString())) {
        this.connection.use { connection ->
            if (readOnly) {
                connection.isReadOnly = true // Setter transaction i read-only
            }
            val dbTransaction = DBTransaction(connection, readOnly)
            dbTransaction.transaction(block)
        }
    }
}

