package no.nav.aap.komponenter.config

/**
 * Lik logikk som [configForKey], men kaster [IllegalStateException] om verdien ikke finnes.
 */
public fun requiredConfigForKey(key: String): String {
    val property = configForKey(key)
    if (property != null) {
        return property
    }
    throw IllegalStateException("Mangler påkrevd config verdi $key")
}

/**
 * Ser etter config-verdier først blant System properties, på format `my.property`.
 * Hvis ikke eksisterer, ser deretter blant miljøvariabler, men på formatet `MY_PROPERTY`.
 *
 * Hvis ingen funnet, returnerer `null`.
 */
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