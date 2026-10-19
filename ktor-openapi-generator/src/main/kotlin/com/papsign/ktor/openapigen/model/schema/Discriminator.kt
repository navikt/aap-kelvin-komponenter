package com.papsign.ktor.openapigen.model.schema

data class Discriminator<T>(val propertyName: String, val mapping: Map<String, String>? = null)
