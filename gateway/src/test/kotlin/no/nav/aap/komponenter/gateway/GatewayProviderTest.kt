package no.nav.aap.komponenter.gateway

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GatewayProviderTest {
    @Test
    fun `provides the same anonymous instance through both lookup overloads`() {
        val gateway = object : TestGateway {
            override fun hent(): String = "testdata"
        }
        val provider = GatewayProvider(GatewayRegistry().register(gateway))

        assertSame(gateway, provider.provide<TestGateway>())
        assertSame(gateway, provider.provide(TestGateway::class))
        assertEquals("testdata", provider.provide<TestGateway>().hent())
    }

    @Test
    fun `instance registration replaces class registration`() {
        val registry = GatewayRegistry().register<FactoryGateway>()
        val gateway = InstanceGateway("testdata")
        val provider = GatewayProvider(registry)

        registry.register(gateway)

        assertSame(gateway, provider.provide<TestGateway>())
    }

    @Test
    fun `class registration replaces instance registration`() {
        val registry = GatewayRegistry().register(InstanceGateway("testdata"))
        val provider = GatewayProvider(registry)

        registry.register<FactoryGateway>()

        assertEquals("factory", provider.provide<TestGateway>().hent())
        assertNotSame(provider.provide<TestGateway>(), provider.provide<TestGateway>())
    }

    @Test
    fun `registering another instance of the same class replaces the instance`() {
        val registry = GatewayRegistry().register(InstanceGateway("first"))
        val provider = GatewayProvider(registry)
        val replacement = InstanceGateway("second")

        registry.register(replacement)

        assertSame(replacement, provider.provide<TestGateway>())
    }

    @Test
    fun `anonymous instance replaces singleton and singleton can replace instance`() {
        val registry = GatewayRegistry().register<SingletonGateway>()
        val provider = GatewayProvider(registry)
        val gateway = object : TestGateway {
            override fun hent(): String = "testdata"
        }

        registry.register(gateway)
        assertSame(gateway, provider.provide<TestGateway>())

        registry.register<SingletonGateway>()
        assertSame(SingletonGateway, provider.provide<TestGateway>())
    }

    @Test
    fun `instance registration preserves unrelated gateways`() {
        val registry = GatewayRegistry().register<SingletonGateway>()
        val gateway = object : OtherGateway {}
        val provider = GatewayProvider(registry.register(gateway))

        assertSame(gateway, provider.provide<OtherGateway>())
        assertSame(SingletonGateway, provider.provide<TestGateway>())
    }

    @Test
    fun `anonymous instance can implement multiple gateway interfaces`() {
        val gateway = object : TestGateway, OtherGateway {
            override fun hent(): String = "testdata"
        }
        val provider = GatewayProvider(GatewayRegistry().register(gateway))

        assertSame(gateway, provider.provide<TestGateway>())
        assertSame(gateway, provider.provide<OtherGateway>())
    }

    @Test
    fun `class registration still requires a factory for non-singletons`() {
        assertThrows<IllegalArgumentException> {
            GatewayRegistry().register<InstanceGateway>()
        }
    }

    interface TestGateway : Gateway {
        fun hent(): String
    }

    interface OtherGateway : Gateway

    class InstanceGateway(private val value: String) : TestGateway {
        override fun hent(): String = value
    }

    class FactoryGateway : TestGateway {
        override fun hent(): String = "factory"

        companion object : Factory<FactoryGateway> {
            override fun konstruer(): FactoryGateway = FactoryGateway()
        }
    }

    object SingletonGateway : TestGateway {
        override fun hent(): String = "singleton"
    }
}
