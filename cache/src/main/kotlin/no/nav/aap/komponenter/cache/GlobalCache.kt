package no.nav.aap.komponenter.cache

import com.github.benmanes.caffeine.cache.Cache
import kotlin.reflect.KClass

internal object GlobalCache {

    private val caches = mutableMapOf<KClass<*>, Cache<Any, Any>>()

    fun getCache(clazz: KClass<*>): Cache<Any, Any> = caches[clazz]!!

    fun hasCache(clazz: KClass<*>) = caches.containsKey(clazz)

    fun putCache(clazz: KClass<*>, cache: Cache<Any, Any>) { caches[clazz] = cache}

    fun resetCache() { caches.clear() }

}