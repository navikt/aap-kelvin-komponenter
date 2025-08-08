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
    /* Må være public pga. inline functions. */
    public val log: Logger = LoggerFactory.getLogger(javaClass)

    init {
        for (gateway in registry) {
            validater(gateway)
        }
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
     * implementasjoner.
     *
     * Slett companion object ingen dependencies bruker denne funksjonaliteten.  */
    public companion object {
        public val singletonProvider: GatewayProvider = GatewayProvider(GatewayRegistry.registry)

        public inline fun <reified T : Gateway> provide(type: KClass<T>): T {
            return singletonProvider.provide()
        }

        public inline fun <reified T : Gateway> provide(): T {
            return singletonProvider.provide()
        }
    }
}