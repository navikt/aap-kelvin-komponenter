package no.nav.aap.komponenter.config

public fun requiredConfigForKey(key: String): String {
    val property = configForKey(key)
    if (property != null) {
        return property
    }
    throw IllegalStateException("Mangler påkrevd config verdi $key")
}

public fun configForKey(key: String): String? {
    var property = System.getProperty(key)
    if (property != null) {
        return property
    }
    val oppdatertKey = key.uppercase().replace(".", "_")
    property = System.getProperty(oppdatertKey)
    if (property != null) {
        return property
    }
    return System.getenv(oppdatertKey)
}