package no.nav.aap.komponenter.dbtest

import java.sql.ConnectionBuilder
import java.sql.ShardingKeyBuilder
import javax.sql.DataSource

/**
 * Oppretter en ny database som kan brukes i tester.
 * Databasen kan brukes parallelt med andre tester.
 *
 * Bruk slik:
 * <pre><code>
 * companion object {
 * private val dataSource = TestDataSource()
 *
 * @AfterAll
 * @JvmStatic
 * fun tearDown() = dataSource.close()
 * }
 * </code></pre>
 *
 * Eller dersom du trenger å nullstille databasens innhold mellom hver test:
 * <pre><code>
 * private lateinit var dataSource: TestDataSource
 *
 * @BeforeEach
 * fun setup() {
 *   dataSource = TestDataSource()
 * }
 *
 * @AfterEach
 * fun tearDown() {
 *   dataSource.close()
 * }
 *   ...
 * </code></pre>
 */
class TestDataSource private constructor(private val delegate: DataSource) : AutoCloseable, DataSource by delegate {
    override fun createConnectionBuilder(): ConnectionBuilder {
        return delegate.createConnectionBuilder()
    }

    override fun createShardingKeyBuilder(): ShardingKeyBuilder {
        return delegate.createShardingKeyBuilder()
    }

    override fun close() {
        InitTestDatabase.closerFor(delegate)
    }

    companion object {
        operator fun invoke(): TestDataSource {
            val delegate = InitTestDatabase.freshDatabase()
            return TestDataSource(delegate)
        }
    }
}
