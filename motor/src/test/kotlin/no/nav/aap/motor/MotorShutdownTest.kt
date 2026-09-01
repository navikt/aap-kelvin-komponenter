package no.nav.aap.motor

import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.help.TullTestJobbUtfører
import no.nav.aap.motor.mdc.NoExtraLogInfoProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class MotorShutdownTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun afterEach() {
        dataSource.close()
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun `alle executors skal være shutdown etter stop`() {
        val motor = MotorImpl(
            dataSource = dataSource,
            antallKammer = 1,
            logInfoProvider = NoExtraLogInfoProvider,
            jobber = listOf(TullTestJobbUtfører),
        )

        motor.start()
        motor.stop()

        for (fieldName in listOf("schedulerExecutor", "watchdogExecutor", "metricExecutor", "executor")) {
            val field = MotorImpl::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            val executor = field.get(motor) as ExecutorService
            assertThat(executor.isShutdown)
                .withFailMessage("Executor '%s' skal være shutdown etter stop()", fieldName)
                .isTrue()
        }
    }
}
