package com.papsign.ktor.openapigen.model

import com.papsign.ktor.openapigen.cleanEmptyValues
import com.papsign.ktor.openapigen.convertToValue
import kotlin.reflect.full.memberProperties

interface DataModel {

    fun serialize(): Map<String, Any?> {
        return this::class.memberProperties.associateBy { it.name }.mapValues { (_, prop) ->
            convertToValue(prop.call(this))
        }.cleanEmptyValues()
    }
}
