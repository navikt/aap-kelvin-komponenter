package no.nav.aap.komponenter.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

interface TestInterface {
    val counter: Int
    fun cachedMethod(): Int
    fun cachedMethod(string: String): Int
    fun uncachedMethod(): Int
    fun cachedNullMethod()
}

class TestClass : TestInterface {

    override var counter = 0

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

    @Cacheable
    override fun cachedNullMethod() {
        counter++
    }


}

fun TestClass.withCache() = withCache(this, TestInterface::class.java)

class CacheProxyTest {

    @AfterEach
    fun tearDown() {
        GlobalCache.resetCache()
    }

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

    @Test
    fun `different arguments to the same method should be cached separately`() {
        val testClass = TestClass().withCache()

        testClass.cachedMethod("SWAG")
        val actual = testClass.cachedMethod("YOLO")

        assertThat(actual).isEqualTo(1)
    }

    @Test
    fun `instances of same class should share cache`() {
        val testClass1 = TestClass().withCache()
        val testClass2 = TestClass().withCache()

        testClass1.cachedMethod()
        val actual = testClass2.cachedMethod()

        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `expect that mathods that returns null are cached`() {
        val testClass = TestClass().withCache()

        testClass.cachedNullMethod()
        testClass.cachedNullMethod()

        assertThat(testClass.counter).isEqualTo(1)
    }
}