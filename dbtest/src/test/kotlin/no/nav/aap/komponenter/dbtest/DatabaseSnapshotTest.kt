package no.nav.aap.komponenter.dbtest

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import javax.sql.DataSource

class DatabaseSnapshotTest {

    @Test
    fun `createSnapshot returns a snapshot that can vend independent copies`() {
        val original = TestDataSource()

        original.connection.use { conn ->
            conn.prepareStatement("INSERT INTO entries (name) VALUES ('initial')").execute()
        }

        // Freeze state — original remains open and usable
        val snapshot = original.createSnapshot()

        try {
            val ds1 = snapshot.newDataSource()
            val ds2 = snapshot.newDataSource()

            try {
                fun countRows(ds: DataSource): Int =
                    ds.connection.use { conn ->
                        conn.prepareStatement("SELECT COUNT(*) FROM entries").executeQuery().use { rs ->
                            rs.next(); rs.getInt(1)
                        }
                    }

                assertThat(countRows(ds1)).isEqualTo(1)
                assertThat(countRows(ds2)).isEqualTo(1)

                // Mutate ds1 — must not affect ds2
                ds1.connection.use { conn ->
                    conn.prepareStatement("INSERT INTO entries (name) VALUES ('extra')").execute()
                }

                assertThat(countRows(ds1)).isEqualTo(2)
                assertThat(countRows(ds2)).isEqualTo(1)
            } finally {
                ds1.close()
                ds2.close()
            }
        } finally {
            snapshot.close()
            original.close()
        }
    }

    @Test
    fun `original datasource remains usable after createSnapshot`() {
        val original = TestDataSource()

        original.connection.use { conn ->
            conn.prepareStatement("INSERT INTO entries (name) VALUES ('before-snapshot')").execute()
        }

        val snapshot = original.createSnapshot()

        try {
            // original should still accept connections and writes after snapshotting
            original.connection.use { conn ->
                conn.prepareStatement("INSERT INTO entries (name) VALUES ('after-snapshot')").execute()
            }

            val names = original.connection.use { conn ->
                conn.prepareStatement("SELECT name FROM entries ORDER BY id").executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                }
            }
            assertThat(names).containsExactly("before-snapshot", "after-snapshot")

            // Snapshot copies should only contain the pre-snapshot row
            val snapshotNames = snapshot.newDataSource().use { ds ->
                ds.connection.use { conn ->
                    conn.prepareStatement("SELECT name FROM entries ORDER BY id").executeQuery().use { rs ->
                        generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                    }
                }
            }
            assertThat(snapshotNames).containsExactly("before-snapshot")
        } finally {
            snapshot.close()
            original.close()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class SetupOnceRestorePerTest {
        private val original = TestDataSource()
        private lateinit var snapshot: DatabaseSnapshot
        private lateinit var dataSource: DataSource

        @BeforeAll
        fun setupOnce() {
            original.connection.use { conn ->
                conn.prepareStatement("INSERT INTO entries (name) VALUES ('setup-once')").execute()
            }
            snapshot = original.createSnapshot()
        }

        @BeforeEach
        fun resetDatabase() {
            dataSource = snapshot.newDataSource()
        }

        @Test
        fun `first test sees snapshot row and own insert`() {
            dataSource.connection.use { conn ->
                conn.prepareStatement("INSERT INTO entries (name) VALUES ('test-1')").execute()
            }
            val names = dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT name FROM entries ORDER BY id").executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                }
            }
            assertThat(names).containsExactly("setup-once", "test-1")
        }

        @Test
        fun `second test sees only snapshot row — independent of first test`() {
            val names = dataSource.connection.use { conn ->
                conn.prepareStatement("SELECT name FROM entries ORDER BY id").executeQuery().use { rs ->
                    generateSequence { if (rs.next()) rs.getString(1) else null }.toList()
                }
            }
            assertThat(names).containsExactly("setup-once")
        }

        @AfterAll
        fun teardownSnapshot() {
            snapshot.close()
        }
    }
}
