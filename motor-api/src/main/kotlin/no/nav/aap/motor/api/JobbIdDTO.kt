package no.nav.aap.motor.api

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

public data class JobbIdDTO(@PathParam("ID") val jobbId: Long)