package no.nav.aap.komponenter.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.reflect.Method


internal fun Caffeine<Any, Any>.maximumsize(method: Method): Caffeine<Any, Any> {
    val cacheableAnnotation = method.annotations.find { it is Cacheable } as Cacheable
    if (cacheableAnnotation.maximumSize > 0) {
        this.maximumSize(cacheableAnnotation.maximumSize)
    }
    return this
}

internal fun Caffeine<Any, Any>.expireAfterWrite(method: Method): Caffeine<Any, Any> {
    val cacheableAnnotation = method.annotations.find { it is Cacheable } as Cacheable
    val expireAfterWriteAnnotation = cacheableAnnotation.expireAfterWrite
    if (expireAfterWriteAnnotation.duration > 0) {
        this.expireAfterWrite(expireAfterWriteAnnotation.duration, expireAfterWriteAnnotation.timeUnit)
    }
    return this
}
