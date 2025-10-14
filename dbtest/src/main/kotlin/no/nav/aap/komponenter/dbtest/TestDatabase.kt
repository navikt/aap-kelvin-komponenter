package no.nav.aap.komponenter.dbtest

import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.TestInstancePostProcessor
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback
import org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields
import javax.sql.DataSource

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(TestDatabaseExtension::class)
public annotation class TestDatabase

public class TestDatabaseExtension : TestInstancePreDestroyCallback, TestInstancePostProcessor {
    override fun postProcessTestInstance(testInstance: Any, context: ExtensionContext) {
        findAnnotatedFields(context.requiredTestClass, TestDatabase::class.java)
            .forEach { field ->
                field.setAccessible(true)
                // Create the DataSource
                field.set(testInstance, InitTestDatabase.freshDatabase())
            }
    }

    override fun preDestroyTestInstance(context: ExtensionContext) {
        TestInstancePreDestroyCallback.preDestroyTestInstances(context) { testInstance ->
            findAnnotatedFields(context.requiredTestClass, TestDatabase::class.java)
                .forEach {
                    // Close the DataSource
                    InitTestDatabase.closerFor(it.get(testInstance) as DataSource)
                }
        }
    }
}

