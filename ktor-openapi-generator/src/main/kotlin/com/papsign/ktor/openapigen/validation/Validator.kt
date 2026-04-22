package com.papsign.ktor.openapigen.validation

interface Validator {
    /**
     * @param subject the serialized property value
     * @return the transformed property, or [subject] if unchanged
     */
    fun <T> validate(subject: T?): T?
}
