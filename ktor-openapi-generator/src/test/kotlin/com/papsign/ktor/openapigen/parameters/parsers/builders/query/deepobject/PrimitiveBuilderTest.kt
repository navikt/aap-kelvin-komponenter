package com.papsign.ktor.openapigen.parameters.parsers.builders.query.deepobject

import com.papsign.ktor.openapigen.parameters.parsers.testSelector
import org.junit.jupiter.api.Test

class PrimitiveBuilderTest {

    @Test
    fun testFloat() {
        val key = "key"
        val expected = 1f
        val parse = mapOf(
            key to listOf("1")
        )
        DeepBuilderFactory.testSelector(expected, key, parse, true)
    }

    @Test
    fun testUInt() {
        val key = "key"
        val expected = 1u
        val parse = mapOf(
            key to listOf("1")
        )
        DeepBuilderFactory.testSelector(expected, key, parse, true)
    }

    @Test
    fun testULong() {
        val key = "key"
        val expected = 1uL
        val parse = mapOf(
            key to listOf("1")
        )
        DeepBuilderFactory.testSelector(expected, key, parse, true)
    }

    @Test
    fun testUShort() {
        val key = "key"
        val expected: UShort = 1u
        val parse = mapOf(
            key to listOf("1")
        )
        DeepBuilderFactory.testSelector(expected, key, parse, true)
    }

    @Test
    fun testUByte() {
        val key = "key"
        val expected: UByte = 1u
        val parse = mapOf(
            key to listOf("1")
        )
        DeepBuilderFactory.testSelector(expected, key, parse, true)
    }
}

