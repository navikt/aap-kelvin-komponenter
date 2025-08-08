package no.nav.aap.komponenter.gateway

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

public class GatewayProvider(
    /* Må være public pga. inline functions. */
    public val registry: Set<KClass<Gateway>>,
) {
    public constructor(body: Builder.() -> Unit) : this(Builder().apply(body).build())

    init {
        for (gateway in registry) {
            validater(gateway)
        }
    }

    public fun status() {
        log.info(
            "{} gateway registrert har følgende '{}'",
            registry.size,
            registry.map { kclass -> kclass.starProjectedType })
    }

    public inline fun <reified T : Gateway> provide(): T {
        val gatewayKlass = registry.singleOrNull { klass -> klass.starProjectedType.isSubtypeOf(T::class.starProjectedType) }
        if (gatewayKlass == null) {
            log.warn("Gateway av typen '{}' er ikke registrert, har følgende '{}'", T::class, registry)
            throw IllegalStateException("Gateway av typen '${T::class}' er ikke registrert")
        }

        val companionObjectType = gatewayKlass.companionObject
        if (companionObjectType == null
            && gatewayKlass.objectInstance != null
            && gatewayKlass.isSubclassOf(Gateway::class)
        ) {
            return gatewayKlass.objectInstance as T
        }

        val companionObject = gatewayKlass.companionObjectInstance
        requireNotNull(companionObject) {
            "Gateway må ha companion object"
        }
        if (companionObject is Factory<*>) {
            return companionObject.konstruer() as T
        }
        throw IllegalStateException("Gateway ${T::class} må ha et companion object som implementerer Factory<T> interfacet.")
    }

    /* Bruk av singleton for å hente gateways burde fases ut, for det
     * gjør det vanskelig å skrive tester som bruker forskjellige
     * implementasjoner. */
    public companion object {
        public val singletonProvider: GatewayProvider = GatewayProvider(GatewayRegistry.registry)

        public inline fun <reified T : Gateway> provide(type: KClass<T>): T {
            return singletonProvider.provide()
        }

        public inline fun <reified T : Gateway> provide(): T {
            return singletonProvider.provide()
        }

        /* Må være public pga. inline functions. */
        public val log: Logger = LoggerFactory.getLogger(GatewayProvider::class.java)

        /* Må være public pga. inline functions. */
        public fun validater(klass: KClass<*>) {
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
    }

    public class Builder {
        public val registry: MutableSet<KClass<Gateway>> = mutableSetOf()

        public inline fun <reified T : Gateway> register() {
            validater(T::class)

            // Kode for å støtte at tester kan legge inn varianter, burde potensielt vært skilt ut?
            val removedSomething = registry.removeIf { klass ->
                T::class.supertypes.filter { type ->
                    type.isSubtypeOf(Gateway::class.starProjectedType)
                }.any { type -> klass.starProjectedType.isSubtypeOf(type) }
            }
            if (removedSomething) {
                log.warn("Gateway '{}' var allerede registrert", T::class)
            }
            @Suppress("UNCHECKED_CAST")
            registry.add(T::class as KClass<Gateway>)
        }

        public fun build(): Set<KClass<Gateway>> {
            return registry.toSet()
        }
    }
}