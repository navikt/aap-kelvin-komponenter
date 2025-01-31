package no.nav.aap.komponenter.cache

import com.github.benmanes.caffeine.cache.Cache
import java.lang.reflect.Method
import kotlin.reflect.KClass

private typealias KeyPair = Pair<KClass<*>, Method>

internal object GlobalCache {

    private val caches = mutableMapOf<KeyPair, Cache<Any, Any>>()

    fun getCache(key: KeyPair): Cache<Any, Any> = caches[key]!!

    fun hasCache(key: KeyPair) = caches.containsKey(key)

    fun putCache(key: KeyPair, cache: Cache<Any, Any>) { caches[key] = cache}

    fun resetCache() { caches.clear() }

}