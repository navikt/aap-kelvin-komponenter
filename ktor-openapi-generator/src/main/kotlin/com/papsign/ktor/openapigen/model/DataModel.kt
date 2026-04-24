package com.papsign.ktor.openapigen.model

import com.papsign.ktor.openapigen.cleanEmptyValues
import com.papsign.ktor.openapigen.convertToValue
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

interface DataModel {

    fun serialize(): Map<String, Any?> {
        return this::class.memberProperties.associateBy { it.name }.mapValues { (_, prop) ->
            @Suppress("UNCHECKED_CAST")
            convertToValue((prop as KProperty1<DataModel, *>).get(this))
        }.cleanEmptyValues()
    }
}
