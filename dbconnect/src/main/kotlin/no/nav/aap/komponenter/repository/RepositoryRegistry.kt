package no.nav.aap.komponenter.repository

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.superclasses

public class RepositoryRegistry(
    internal val registry: ArrayList<Pair<KClass<Repository>, KClass<Repository>>>,
) {
    public constructor() : this(ArrayList())

    private val logger = LoggerFactory.getLogger(RepositoryRegistry::class.java)!!

    public inline fun <reified T : Repository> register(): RepositoryRegistry {
        return register(T::class)
    }

    public fun <T : Repository> register(repository: KClass<T>): RepositoryRegistry {
        validater(repository)

        // Kode for å støtte at tester kan legge inn varianter, burde potensielt vært skilt ut?
        val removedSomething = registry.removeIf { (markerInterface, _) ->
            markerInterface in repository.markerInterfaces
        }
        if (removedSomething) {
            logger.warn("Repository '{}' hadde en variant allerede registrert", repository)
        }
        registry.addAll(repository.markerInterfaces.map {
            @Suppress("UNCHECKED_CAST")
            (it to repository) as Pair<KClass<Repository>, KClass<Repository>>
        })
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

    internal fun fetch(ktype: KClass<*>): KClass<Repository> {
        val singleOrNull = registry.singleOrNull { (marker, _) ->
            marker == ktype
        }
        if (singleOrNull == null) {
            logger.warn("Repository av typen '{}' er ikke registrert, har følgende '{}'", ktype, registry)
            throw IllegalStateException("Repository av typen '$ktype' er ikke registrert")
        }

        return singleOrNull.second
    }

    internal fun alle(): List<KClass<Repository>> {
        return registry.map {
            it.second
        }
    }

    public fun status() {
        logger.info(
            "{} repositories registrert har følgende '{}'",
            registry.size,
            registry.map { kclass -> kclass.second.starProjectedType })
    }

    public fun provider(connection: DBConnection): RepositoryProvider {
        return RepositoryProvider(connection, this)
    }
}

private val KClass<*>.markerInterfaces: Set<KClass<*>>
    get() =
        this.superclasses.filter { Repository::class in it.allSuperclasses }.toSet()
            .also {
                check(it.isNotEmpty()) { "${this.simpleName} har ingen Repository-marker interface" }
            }
