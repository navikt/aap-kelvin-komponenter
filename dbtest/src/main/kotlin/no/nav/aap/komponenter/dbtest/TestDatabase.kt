package no.nav.aap.komponenter.dbtest

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback
import org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields
import java.io.Closeable

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(TestDatabaseExtension::class)
public annotation class TestDatabase

public class TestDatabaseExtension : TestInstancePreDestroyCallback, TestInstancePostProcessor {
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        findAnnotatedFields(context.requiredTestClass, TestDatabase::class.java)
            .forEach { field ->
                field.setAccessible(true)
                field.set(testInstance, InitTestDatabase.freshDatabase())
            }
    }

    override fun preDestroyTestInstance(context: ExtensionContext) {
        TestInstancePreDestroyCallback.preDestroyTestInstances(context) { testInstance ->
            findAnnotatedFields(context.requiredTestClass, TestDatabase::class.java)
                .forEach {
                    (it.get(testInstance) as Closeable).close()
                }
        }
    }
}

