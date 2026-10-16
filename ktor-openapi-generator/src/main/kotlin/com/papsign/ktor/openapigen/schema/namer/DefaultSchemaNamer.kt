package com.papsign.ktor.openapigen.schema.namer

import kotlin.reflect.KType

object DefaultSchemaNamer : SchemaNamer {
    // Kun <*>-suffiks fjernes; konkrete type-argumenter må beholdes for å unngå navnekollisjoner
    private val starProjections = Regex("<[*, ]+>")

    override fun get(type: KType): String = type.toString().replace(starProjections, "")
}
