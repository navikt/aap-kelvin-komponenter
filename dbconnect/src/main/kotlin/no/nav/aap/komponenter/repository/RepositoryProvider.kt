package no.nav.aap.komponenter.repository

import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.reflect.KClass
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

public class RepositoryProvider internal constructor (
    private val connection: DBConnection,
    private val repositoryRegistry: RepositoryRegistry,
) {

    public inline fun <reified T : Repository> provide(): T {
        return provide(T::class)
    }

    public fun <T : Repository> provide(klass: KClass<T>): T {
        val repositoryKlass = repositoryRegistry.fetch(klass.starProjectedType)
        return internalCreate(repositoryKlass)
    }

    public fun provideAlle(): List<Repository> {
        return repositoryRegistry.alle().map { klass -> internalCreate(klass) }
    }

    private fun<T: Repository> internalCreate(repositoryKlass: KClass<Repository>): T {
        val companionObjectType = repositoryKlass.companionObject
        if (companionObjectType == null && repositoryKlass.objectInstance != null
            && repositoryKlass.isSubclassOf(Repository::class)
        ) {
            @Suppress("UNCHECKED_CAST")
            return repositoryKlass.objectInstance as T
        }

        val companionObject = repositoryKlass.companionObjectInstance
        requireNotNull(companionObject) {
            "Repository må ha companion object"
        }
        if (companionObject is RepositoryFactory<*>) {
            @Suppress("UNCHECKED_CAST")
            return companionObject.konstruer(connection) as T
        }
        throw IllegalStateException("Repository må ha et companion object som implementerer Factory<T> interfacet.")
    }
}
