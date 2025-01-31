package no.nav.aap.komponenter.cache

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

interface TestInterface {
    val counter: Int
    fun cachedMethod(): Int
    fun cachedMethod(string: String): Int
    fun uncachedMethod(): Int
    fun cachedNullMethod()
    fun zeroCacheSizeMethod(int: Int): Int
    fun oneCacheSizeMethod(int: Int): Int
    fun cachedMultiArgMethod(int1: Int, int2: Int): Int
}

class TestClass : TestInterface {

    override var counter = 0

    @Cacheable
    override fun cachedMethod() = counter++

    @Cacheable
    override fun cachedMethod(string: String) = counter++

    override fun uncachedMethod() = counter++

    @Cacheable
    override fun cachedNullMethod() {
        counter++
    }

    @Cacheable(maximumSize = 0)
    override fun zeroCacheSizeMethod(int: Int) = counter++

    @Cacheable(maximumSize = 1)
    override fun oneCacheSizeMethod(int: Int) = counter++

    @Cacheable
    override fun cachedMultiArgMethod(int1: Int, int2: Int) = counter++

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

        testClass.cachedMethod("YOLO")
        testClass.cachedMethod("YOLO")
        testClass.cachedMethod("YOLO")
        testClass.cachedMethod("YOLO")
        testClass.cachedMethod("YOLO")
        val actual = testClass.cachedMethod("YOLO")

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

    @Test
    fun `dersom cachesize er 0 skal det ikke være begrensninger på cachesize`() {
        val testClass = TestClass().withCache()

        testClass.zeroCacheSizeMethod(0)
        testClass.zeroCacheSizeMethod(1)
        val actual = testClass.zeroCacheSizeMethod(0)

        assertThat(actual).isEqualTo(0)
    }

    @Test
    fun `dersom cachesize er 1 skal cache bli resatt annenhvert kall`() {
        val testClass = TestClass().withCache()
        thread {
            testClass.oneCacheSizeMethod(1)
            testClass.oneCacheSizeMethod(2)
            testClass.oneCacheSizeMethod(3)

            Thread.sleep(100)

            val notCached = testClass.oneCacheSizeMethod(1)

            assertThat(notCached).isEqualTo(2)
        }
    }

    @Test
    fun `rekkefølge for argumenter skal påvirke caching`() {
        val testClass = TestClass().withCache()
        testClass.cachedMultiArgMethod(1, 2)
        val notCached = testClass.cachedMultiArgMethod(2, 1)

        assertThat(notCached).isEqualTo(1)
    }

    @Test
    fun `metoder med flere argumenter skal caches`() {
        val testClass = TestClass().withCache()

        testClass.cachedMultiArgMethod(1, 2)
        val cached = testClass.cachedMultiArgMethod(1, 2)

        assertThat(cached).isEqualTo(0)
    }


}
