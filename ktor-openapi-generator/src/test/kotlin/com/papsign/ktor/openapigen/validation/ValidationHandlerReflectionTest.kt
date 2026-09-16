package com.papsign.ktor.openapigen.validation

import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for the reflection-based property access used to apply validators
 * (e.g. @Min/@Max/@Clamp) to properties on plain (non-data) classes and private properties.
 *
 * Kotlin value classes (like the unsigned integer types UInt/ULong/UShort/UByte) are stored
 * as their unboxed underlying primitive on the JVM but must be read/written as properly
 * boxed instances through Kotlin reflection, not raw java.lang.reflect.Field/Method access.
 */
class ValidationHandlerReflectionTest {

    // A regular (non-data) class with a mutable, annotated var property and no copy()
    // function, so the validator must mutate the field in place via the Kotlin property
    // setter rather than going through copy.callBy.
    class MutableUIntHolder(@Min(1) var value: UInt)

    class PrivatePropertyHolder(@Min(1) private val value: Int) {
        fun getValue() = value
    }

    // A regular (non-data) class with a read-only (val) unsigned property and no copy()
    // function, so the validator falls back to java.lang.reflect.Field.set and must unbox
    // the boxed UInt to the field's underlying `int` representation itself.
    class ReadOnlyUIntHolder(@Min(1) val value: UInt)

    @Test
    fun `validerer og oppdaterer mutable UInt-egenskap uten copy()`() {
        val handler = ValidationHandler.build(MutableUIntHolder::class)
        val holder = MutableUIntHolder(5u)
        val result = handler.handle(holder)
        assertEquals(5u, result.value)
    }

    @Test
    fun `kaster ved brudd på @Min for mutable UInt-egenskap uten copy()`() {
        val handler = ValidationHandler.build(MutableUIntHolder::class)
        val holder = MutableUIntHolder(0u)
        assertFailsWith<Exception> {
            handler.handle(holder)
        }
    }

    @Test
    fun `kaster ikke ved validering av privat egenskap`() {
        // Building/handling a class with an annotated private property should not throw
        // IllegalCallableAccessException.
        val handler = ValidationHandler.build(PrivatePropertyHolder::class)
        val holder = PrivatePropertyHolder(5)
        val result = handler.handle(holder)
        assertEquals(5, result.getValue())
    }

    @Test
    fun `kaster ved brudd på @Min for privat egenskap`() {
        val handler = ValidationHandler.build(PrivatePropertyHolder::class)
        val holder = PrivatePropertyHolder(0)
        assertFailsWith<Exception> {
            handler.handle(holder)
        }
    }

    @Test
    fun `validerer og oppdaterer read-only UInt-egenskap uten copy()`() {
        val handler = ValidationHandler.build(ReadOnlyUIntHolder::class)
        val holder = ReadOnlyUIntHolder(5u)
        val result = handler.handle(holder)
        assertEquals(5u, result.value)
    }

    @Test
    fun `kaster ved brudd på @Min for read-only UInt-egenskap uten copy()`() {
        val handler = ValidationHandler.build(ReadOnlyUIntHolder::class)
        val holder = ReadOnlyUIntHolder(0u)
        assertFailsWith<Exception> {
            handler.handle(holder)
        }
    }
}
