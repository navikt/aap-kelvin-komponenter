package no.nav.aap.komponenter.repository

import no.nav.aap.komponenter.dbconnect.DBConnection

/**
 * Factory interface for repository companion object
 */
public interface RepositoryFactory<T : Repository> {
    public fun konstruer(connection: DBConnection): T
}