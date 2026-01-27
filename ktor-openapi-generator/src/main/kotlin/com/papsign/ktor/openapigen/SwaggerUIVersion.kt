package com.papsign.ktor.openapigen

import java.util.Properties

object SwaggerUIVersion {

    private val log = classLogger()

    // TODO: unngå manuell oppdatering
    val version: String

    init {
        val file = this::class.java.getResourceAsStream("/META-INF/maven/org.webjars/swagger-ui/pom.properties")
        if(file != null) {

            val properties = Properties()
            properties.load(file)
            version = properties.getProperty("version")
        } else {
            log.warn("Klarte ikke hente versjon på swagger UI via avhengigheten")
            version = "5.20.7"
        }
    }
}
