package no.nav.aap.komponenter.dbconnect

import org.slf4j.LoggerFactory
import java.sql.Connection

internal class DBTransaction(connection: Connection, private val readOnly: Boolean) {
    private val dbConnection: DBConnection = DBConnection(connection)

    private companion object {
        private val log = LoggerFactory.getLogger(DBTransaction::class.java)
    }

    internal fun <T> transaction(block: (DBConnection) -> T): T {
        try {
            dbConnection.autoCommitOff()
            val result = block(dbConnection)
            // Commit må kalles på i readOnly og read/write for å frigjøre eventuelle låser
            dbConnection.commit()
            return result
        } catch (e: Throwable) {
            if (!readOnly) {
                log.info("Kjører rollback etter feil. Feil-type: ${e.javaClass.name}")
            }
            // Kaller fortsatt på rollback for en readOnly transaction, må frigjøre eventuelle databaselåser
            dbConnection.rollback()
            throw e
        } finally {
            dbConnection.autoCommitOn()
        }
    }
}
