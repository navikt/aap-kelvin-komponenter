package no.nav.aap.komponenter.gateway

import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

public class GatewayProvider internal constructor(
    private val gatewayRegistry: GatewayRegistry
) {

    public fun <T : Gateway> provide(type: KClass<T>): T {
        val gatewayKlass = gatewayRegistry.fetch(type.starProjectedType)

        return internalCreate(gatewayKlass)
    }

    public inline fun <reified T : Gateway> provide(): T {
        return provide(T::class)
    }

    private fun <T : Gateway> internalCreate(gatewayKlass: KClass<Gateway>): T {
        val companionObjectType = gatewayKlass.companionObject
        if (companionObjectType == null && gatewayKlass.objectInstance != null
            && gatewayKlass.isSubclassOf(Gateway::class)
        ) {
            @Suppress("UNCHECKED_CAST")
            return gatewayKlass.objectInstance as T
        }

        val companionObject = gatewayKlass.companionObjectInstance
        requireNotNull(companionObject) {
            "Gateway må ha companion object"
        }
        if (companionObject is Factory<*>) {
            @Suppress("UNCHECKED_CAST")
            return companionObject.konstruer() as T
        }
        throw IllegalStateException("Gateway må ha et companion object som implementerer Factory<T> interfacet.")
    }

}