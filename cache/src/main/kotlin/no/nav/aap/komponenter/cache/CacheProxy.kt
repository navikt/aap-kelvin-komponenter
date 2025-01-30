package no.nav.aap.komponenter.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
public annotation class Cacheable


private class CacheProxy(private val subject: Any) : InvocationHandler {

    val cache = Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build<Any, Any>()


    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        if (!getsubjectMethod(method!!).isAnnotationPresent(Cacheable::class.java)) { return callActual(method, args) }

        return cache.getIfPresent(getCacheKey(method, args))?.let { return it } ?:
            putAndReturn(method, args)
    }

    private fun getCacheKey(method: Method, args: Array<out Any>?) = Pair(method, args)

    private fun putAndReturn(method: Method, args: Array<out Any>?): Any {
        val result = callActual(method, args)
        cache.put(getCacheKey(method, args), result!!)
        return result
    }

    private fun callActual(method: Method?, args: Array<out Any>?) =if (args != null) {
        method?.invoke(subject, *args)
    } else {
        method?.invoke(subject)
    }

    private fun getsubjectMethod(method: Method) =
        subject.javaClass.getMethod(method.name, *method.parameterTypes)

}


public fun<T> withCache(subject: T, clazz: Class<T>): T {
    return Proxy.newProxyInstance(
        CacheProxy::class.java.classLoader,
        arrayOf(clazz),
        CacheProxy(subject as Any)
    ) as T
}


