package no.nav.aap.komponenter.dbconnect

import javax.sql.DataSource

public fun <T> DataSource.transaction(readOnly: Boolean = false, block: (DBConnection) -> T): T {
    return this.connection.use { connection ->
        if (readOnly) {
            connection.isReadOnly = true // Setter transaction i read-only
        }
        val dbTransaction = DBTransaction(connection, readOnly)
        dbTransaction.transaction(block)
    }
}
