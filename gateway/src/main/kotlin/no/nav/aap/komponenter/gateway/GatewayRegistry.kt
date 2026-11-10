package no.nav.aap.komponenter.gateway

import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.superclasses

public open class GatewayRegistry {

    private val log = LoggerFactory.getLogger(javaClass)
    private val registry = HashMap<KClass<Gateway>, Gateway?>()
    private val lock = Any()

    public inline fun <reified T : Gateway> register(): GatewayRegistry {
        return register(T::class)
    }

    public fun <T : Gateway> register(repository: KClass<T>): GatewayRegistry {
        validater(repository)
        return register(repository, null)
    }

    /**
     * Registers an existing instance, including anonymous gateway implementations used in tests.
     * The provider returns this same instance on every lookup.
     * Replaces any registration sharing a gateway marker interface.
     */
    public fun register(gateway: Gateway): GatewayRegistry {
        return register(gateway::class, gateway)
    }

    private fun register(repository: KClass<out Gateway>, instance: Gateway?): GatewayRegistry {
        synchronized(lock) {
            val removedSomething = registry.keys.removeIf { klass ->
                klass.markerInterfaces.any { it in repository.markerInterfaces }
            }

            if (removedSomething) {
                log.warn("Gateway '{}' var allerede registrert", repository)
            }
            @Suppress("UNCHECKED_CAST")
            registry[repository as KClass<Gateway>] = instance
        }
        return this
    }

    private fun validater(klass: KClass<*>) {
        require(klass.starProjectedType.isSubtypeOf(Gateway::class.starProjectedType)) {
            "Gateway må være av variant Gateway"
        }
        val companionObject = klass.companionObject
        if (companionObject == null && klass.objectInstance != null) {
            return
        }
        requireNotNull(companionObject) {
            "Gateway må ha companion object"
        }
        require(companionObject.isSubclassOf(Factory::class)) {
            "Gateway må ha companion object av typen Factory"
        }
    }

    public fun fetch(ktype: KType): KClass<Gateway> {
        synchronized(lock) {
            val singleOrNull = registry.keys.singleOrNull { klass -> klass.starProjectedType.isSubtypeOf(ktype) }
            if (singleOrNull == null) {
                log.warn("Gateway av typen '{}' er ikke registrert, har følgende '{}'", ktype, registry.keys)
                throw IllegalStateException("Gateway av typen '$ktype' er ikke registrert")
            }
            return singleOrNull
        }
    }

    @PublishedApi
    internal fun fetchInstance(klass: KClass<Gateway>): Gateway? {
        synchronized(lock) {
            return registry[klass]
        }
    }

    public fun status() {
        log.info(
            "{} gateway registrert har følgende '{}'",
            registry.size,
            registry.keys.map { kclass -> kclass.starProjectedType })
    }
}

private val KClass<*>.markerInterfaces: Set<KClass<*>>
    get() =
        this.superclasses.filter { Gateway::class in it.allSuperclasses }.toSet()
            .also {
                check(it.isNotEmpty()) { "${this.simpleName} har ingen Gateway-marker interface" }
            }