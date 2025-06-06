package no.nav.aap.komponenter.repository

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

public class RepositoryRegistry {
    private val logger = LoggerFactory.getLogger(RepositoryRegistry::class.java)!!
    private val registry = HashSet<KClass<Repository>>()
    private val lock = Object()

    public inline fun <reified T : Repository> register(): RepositoryRegistry {
        return register(T::class)
    }

    public fun <T: Repository> register(repository: KClass<T>): RepositoryRegistry {
        validater(repository)

        synchronized(lock) {
            // Kode for å støtte at tester kan legge inn varianter, burde potensielt vært skilt ut?
            val removedSomething = registry.removeIf { klass ->
                repository.supertypes.filter { type ->
                    type.isSubtypeOf(Repository::class.starProjectedType)
                }.any { type -> klass.starProjectedType.isSubtypeOf(type) }
            }
            if (removedSomething) {
                logger.warn("Repository '{}' hadde en variant allerede registrert", repository)
            }
            @Suppress("UNCHECKED_CAST")
            registry.add(repository as KClass<Repository>)
        }
        return this
    }

    private fun validater(klass: KClass<*>) {
        require(klass.starProjectedType.isSubtypeOf(Repository::class.starProjectedType)) {
            "Repository må være av variant Repository"
        }
        val companionObject = klass.companionObject
        if (companionObject == null && klass.objectInstance != null) {
            return
        }
        requireNotNull(companionObject) {
            "Repository må ha companion object"
        }
        require(companionObject.isSubclassOf(RepositoryFactory::class)) {
            "Repository må ha companion object av typen Factory"
        }
    }

    internal fun fetch(ktype: KType): KClass<Repository> {
        synchronized(lock) {
            val singleOrNull = registry.singleOrNull { klass -> klass.starProjectedType.isSubtypeOf(ktype) }
            if (singleOrNull == null) {
                logger.warn("Repository av typen '{}' er ikke registrert, har følgende '{}'", ktype, registry)
                throw IllegalStateException("Repository av typen '$ktype' er ikke registrert")
            }
            return singleOrNull
        }
    }

    internal fun alle(): List<KClass<Repository>> {
        return registry.toList()
    }

    public fun status() {
        logger.info(
            "{} repositories registrert har følgende '{}'",
            registry.size,
            registry.map { kclass -> kclass.starProjectedType })
    }

    public fun provider(connection: DBConnection): RepositoryProvider {
        return RepositoryProvider(connection, this)
    }
}