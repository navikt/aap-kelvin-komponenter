package com.papsign.ktor.openapigen

import com.fasterxml.jackson.annotation.JsonIgnore


enum class BehovType(@JsonIgnore val valideringsFunksjon: BehovType.() -> Unit) {
    MANUELT_PÅKREVD(BehovType::doNothing),
    MANUELT_FRIVILLIG(BehovType::doNothing),
    VENTEPUNKT(BehovType::doNothing);

    private fun doNothing() {

    }
}
