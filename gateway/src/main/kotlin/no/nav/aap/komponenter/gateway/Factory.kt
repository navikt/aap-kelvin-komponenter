package no.nav.aap.komponenter.gateway

/**
 * Factory interface for gateway companion object
 */
public interface Factory<T : Gateway> {
    public fun konstruer(): T
}