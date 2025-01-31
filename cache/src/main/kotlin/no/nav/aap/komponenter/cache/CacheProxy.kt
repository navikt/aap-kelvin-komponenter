package no.nav.aap.komponenter.cache

import com.github.benmanes.caffeine.cache.Caffeine
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
public annotation class Cacheable


private const val NO_OP = "NO_OP"

private class CacheProxy(private val subject: Any) : InvocationHandler {

    init {
        subject::class.java.methods.forEach { method ->
            if (method.isAnnotationPresent(Cacheable::class.java)) {
                createCache(method)
            }
        }
    }

    override fun invoke(proxy: Any?, method: Method?, args: Array<out Any>?): Any? {
        if (!getsubjectMethod(method!!).isAnnotationPresent(Cacheable::class.java)) {
            return callActual(method, args)
        }

        val result = getCache(method).getIfPresent(getCacheKey(method, args))
        return if (result == null) putAndReturn(method, args)
        else result.let { if (it == NO_OP) null else it }
    }

    private fun getCacheKey(method: Method, args: Array<out Any>?) = Pair(method, args)

    private fun putAndReturn(method: Method, args: Array<out Any>?): Any {
        val result = callActual(method, args) ?: NO_OP
        getCache(method).put(getCacheKey(method, args), result)
        return result
    }

    private fun getCache(method: Method) = GlobalCache.getCache(Pair(
        subject::class,
        subject::class.java.getMethod(method.name, *method.parameterTypes))
    )

    private fun callActual(method: Method?, args: Array<out Any>?) = if (args != null) {
        method?.invoke(subject, *args)
    } else {
        method?.invoke(subject)
    }

    private fun getsubjectMethod(method: Method) =
        subject.javaClass.getMethod(method.name, *method.parameterTypes)

    private fun createCache(method: Method) {
        val key = Pair(subject::class, method)
        if (!GlobalCache.hasCache(key)) {
            GlobalCache.putCache(
                key,
                Caffeine.newBuilder()
                    .expireAfterWrite(2, TimeUnit.MINUTES)
                    .build()
            )
        }
    }

}


public fun <T> withCache(subject: T, clazz: Class<T>): T {
    return Proxy.newProxyInstance(
        CacheProxy::class.java.classLoader,
        arrayOf(clazz),
        CacheProxy(subject as Any)
    ) as T
}


