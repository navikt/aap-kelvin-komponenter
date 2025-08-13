package no.nav.aap.komponenter.gateway

import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

public open class GatewayProvider(
    public val gatewayRegistry: GatewayRegistry,
) {

    public inline fun <reified T : Gateway> provide(type: KClass<T>): T {
        val gatewayKlass = gatewayRegistry.fetch(type.starProjectedType)

        return internalCreate(gatewayKlass)
    }

    public inline fun <reified T : Gateway> provide(): T {
        val repositoryKlass = gatewayRegistry.fetch(T::class.starProjectedType)

        return internalCreate(repositoryKlass)
    }

    public inline fun <reified T : Gateway> internalCreate(gatewayKlass: KClass<Gateway>): T {
        val companionObjectType = gatewayKlass.companionObject
        if (companionObjectType == null && gatewayKlass.objectInstance != null
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
        throw IllegalStateException("Gateway må ha et companion object som implementerer Factory<T> interfacet.")
    }

    @Deprecated("Ikke bruk global GatewayProvider, men inject egen instans.")
    public companion object: GatewayProvider(GatewayRegistry)
}