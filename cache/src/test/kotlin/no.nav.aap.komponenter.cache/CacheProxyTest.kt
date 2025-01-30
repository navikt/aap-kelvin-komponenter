package no.nav.aap.komponenter.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

interface TestInterface {
    fun cachedMethod(): Int
    fun cachedMethod(string: String): Int
    fun uncachedMethod(): Int
}

class TestClass : TestInterface {

    private var counter = 0

    @Cacheable
    override fun cachedMethod(): Int {
        return counter++
    }

    @Cacheable
    override fun cachedMethod(string: String): Int {
        return counter++
    }

    override fun uncachedMethod(): Int {
        return counter++
    }
}

fun TestClass.withCache() = withCache(this, TestInterface::class.java)

class CacheProxyTest {

    @Test
    fun `cached method should not be incremented`() {
        val testClass = TestClass().withCache()

        testClass.cachedMethod()
        val actual = testClass.cachedMethod()

        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `method without cacheable annotation should increment the return value`() {
        val testClass = TestClass().withCache()

        testClass.uncachedMethod()
        val actual = testClass.uncachedMethod()

        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `an overloaded method should not affect the cached result of another`() {
        val testClass = TestClass().withCache()

        testClass.cachedMethod()
        val actual = testClass.cachedMethod("YOLO")

        assertThat(actual).isEqualTo(1)
    }
}