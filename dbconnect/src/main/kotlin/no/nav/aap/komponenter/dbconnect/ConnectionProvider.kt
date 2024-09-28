package no.nav.aap.komponenter.dbconnect


public object ConnectionProvider {

    private var activeConnection = ThreadLocal<DBConnection>()

    internal fun setConnection(dbConnection: DBConnection) {
        if (activeConnection.get() != null) {
            throw IllegalStateException("Det er allerede en aktiv transaksjon, kan ikke opprette en ny.")
        }
        activeConnection.set(dbConnection)
    }

    internal fun clearConnection() {
        activeConnection.set(null)
    }

    public fun activeConnection(): DBConnection {
        val connection = activeConnection.get()
        if (connection == null) {
            throw NotInATransactionException("There is no active transaction")
        }
        return connection
    }
}