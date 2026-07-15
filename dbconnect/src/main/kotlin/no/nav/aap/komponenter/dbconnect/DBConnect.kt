package no.nav.aap.komponenter.dbconnect

import javax.sql.DataSource


/**
 * Start en transaksjon.
 *
 * @param readOnly Om transaksjonen skal være i read-only modus. Defaulter til false.
 */
public fun <T> DataSource.transaction(readOnly: Boolean = false, block: (DBConnection) -> T): T =
    transaction(name = "transaction", readOnly = readOnly, block = block)

/**
 * Start en transaksjon med et navn som vises i tracing.
 *
 * @param name Navn på span i tracing. Bør beskrive hva transaksjonen gjør, f.eks. "hent-søknad".
 * @param readOnly Om transaksjonen skal være i read-only modus. Defaulter til false.
 */
public fun <T> DataSource.transaction(name: String, readOnly: Boolean = false, block: (DBConnection) -> T): T {
    return span(name, listOf("readOnly" to readOnly.toString())) {
        this.connection.use { connection ->
            if (readOnly) {
                connection.isReadOnly = true // Setter transaction i read-only
            }
            val dbTransaction = DBTransaction(connection, readOnly)
            dbTransaction.transaction(block)
        }
    }
}

