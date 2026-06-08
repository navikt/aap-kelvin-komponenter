package no.nav.aap.motor.jobbarkiv

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class ArkiverFerdigstilteJobbRepositoryTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `skal ikke arkivere jobber når arkivtabeller mangler`() {
        dataSource.transaction { connection ->
            val gammelFerdigJobbId =
                opprettJobb(connection, JobbStatus.FERDIG, LocalDateTime.now().minusDays(61), "mangler-arkiv")
            opprettHistorikk(connection, gammelFerdigJobbId, JobbStatus.FERDIG)

            ArkiverFerdigstilteJobb(ArkiverFerdigstilteJobberRepository(connection)).utfør(
                JobbInput(ArkiverFerdigstilteJobb.Companion)
            )

            assertThat(ArkiverFerdigstilteJobberRepository(connection).arkivtabellerFinnes()).isFalse()
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB WHERE id = $gammelFerdigJobbId"
                )
            ).isEqualTo(1)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB_HISTORIKK WHERE jobb_id = $gammelFerdigJobbId"
                )
            ).isEqualTo(1)
        }
    }

    @Test
    fun `skal kun arkivere ferdige jobber eldre enn 60 dager`() {
        dataSource.transaction { connection ->
            opprettArkivtabeller(connection)
            val gammelFerdigJobbId =
                opprettJobb(connection, JobbStatus.FERDIG, LocalDateTime.now().minusDays(61), "eldre-ferdig")
            val nyFerdigJobbId =
                opprettJobb(connection, JobbStatus.FERDIG, LocalDateTime.now().minusDays(10), "nyere-ferdig")
            val gammelKlarJobbId =
                opprettJobb(connection, JobbStatus.KLAR, LocalDateTime.now().minusDays(61), "eldre-klar")

            opprettHistorikk(connection, gammelFerdigJobbId, JobbStatus.KLAR)
            opprettHistorikk(connection, gammelFerdigJobbId, JobbStatus.FERDIG)
            opprettHistorikk(connection, nyFerdigJobbId, JobbStatus.FERDIG)
            opprettHistorikk(connection, gammelKlarJobbId, JobbStatus.KLAR)

            val antallArkiverte = ArkiverFerdigstilteJobberRepository(connection).arkiverFerdigstilteJobber(20)

            assertThat(antallArkiverte).isEqualTo(1)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB WHERE id = $gammelFerdigJobbId"
                )
            ).isZero()
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB WHERE id = $nyFerdigJobbId"
                )
            ).isEqualTo(1)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB WHERE id = $gammelKlarJobbId"
                )
            ).isEqualTo(1)

            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM jobb_arkiv WHERE id = $gammelFerdigJobbId"
                )
            ).isEqualTo(1)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM JOBB_HISTORIKK WHERE jobb_id = $gammelFerdigJobbId"
                )
            ).isZero()
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM jobb_historikk_arkiv WHERE jobb_id = $gammelFerdigJobbId"
                )
            ).isEqualTo(2)
        }
    }

    @Test
    fun `skal kun prosessere så mange som batchen tillater`() {
        dataSource.transaction { connection ->
            opprettArkivtabeller(connection)
            val cutoff = LocalDateTime.now().minusDays(61)

            connection.execute(
                """
                INSERT INTO JOBB (status, type, neste_kjoring)
                SELECT '${JobbStatus.FERDIG.name}', 'batch-jobb', ?
                FROM generate_series(1, 170)
                """.trimIndent()
            ) {
                setParams {
                    setLocalDateTime(1, cutoff)
                }
            }

            connection.execute(
                """
                INSERT INTO JOBB_HISTORIKK (jobb_id, status, opprettet_tid)
                SELECT id, '${JobbStatus.FERDIG.name}', CURRENT_TIMESTAMP
                FROM JOBB
                WHERE type = 'batch-jobb'
                """.trimIndent()
            )

            val repository = ArkiverFerdigstilteJobberRepository(connection)
            val arkiverteDenneRunden = repository.arkiverFerdigstilteJobber(100)

            assertThat(arkiverteDenneRunden).isEqualTo(100)
            assertThat(antall(connection, "SELECT count(*) AS antall FROM JOBB WHERE type = 'batch-jobb'")).isEqualTo(70)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM jobb_arkiv WHERE type = 'batch-jobb'"
                )
            ).isEqualTo(100)
        }
    }

    @Test
    fun `skal kun prosessere en batch`() {
        dataSource.transaction { connection ->
            opprettArkivtabeller(connection)
            val cutoff = LocalDateTime.now().minusDays(61)

            connection.execute(
                """
                INSERT INTO JOBB (status, type, neste_kjoring)
                SELECT '${JobbStatus.FERDIG.name}', 'batch-jobb', ?
                FROM generate_series(1, 50_001)
                """.trimIndent()
            ) {
                setParams {
                    setLocalDateTime(1, cutoff)
                }
            }

            connection.execute(
                """
                INSERT INTO JOBB_HISTORIKK (jobb_id, status, opprettet_tid)
                SELECT id, '${JobbStatus.FERDIG.name}', CURRENT_TIMESTAMP
                FROM JOBB
                WHERE type = 'batch-jobb'
                """.trimIndent()
            )

            val arkiverFerdigstilteJobb = ArkiverFerdigstilteJobb(ArkiverFerdigstilteJobberRepository(connection))

            arkiverFerdigstilteJobb.utfør(JobbInput(ArkiverFerdigstilteJobb))

            assertThat(antall(connection, "SELECT count(*) AS antall FROM JOBB WHERE type = 'batch-jobb'")).isEqualTo(1)
            assertThat(
                antall(
                    connection,
                    "SELECT count(*) AS antall FROM jobb_arkiv WHERE type = 'batch-jobb'"
                )
            ).isEqualTo(50_000)
        }
    }

    private fun opprettArkivtabeller(connection: DBConnection) {
        connection.execute(
            """
            CREATE TABLE jobb_arkiv
            (
                ID            BIGINT                                 NOT NULL PRIMARY KEY,
                STATUS        VARCHAR(50)                            NOT NULL,
                TYPE          VARCHAR(50)                            NOT NULL,
                SAK_ID        BIGINT NULL,
                BEHANDLING_ID BIGINT NULL,
                parameters    text NULL,
                payload       text NULL,
                NESTE_KJORING TIMESTAMP(3)                           NOT NULL,
                OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
            )
            """.trimIndent()
        )

        connection.execute(
            """
            CREATE TABLE jobb_historikk_arkiv
            (
                ID            BIGINT                                 NOT NULL PRIMARY KEY,
                JOBB_ID       BIGINT                                 NOT NULL,
                STATUS        VARCHAR(50)                            NOT NULL,
                FEILMELDING   TEXT NULL,
                OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun opprettJobb(
        connection: DBConnection,
        status: JobbStatus,
        nesteKjoring: LocalDateTime,
        type: String
    ): Long {
        return connection.executeReturnKey(
            """
            INSERT INTO JOBB (status, type, neste_kjoring)
            VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setEnumName(1, status)
                setString(2, type)
                setLocalDateTime(3, nesteKjoring)
            }
        }
    }

    private fun opprettHistorikk(connection: DBConnection, jobbId: Long, status: JobbStatus) {
        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK (jobb_id, status, opprettet_tid)
            VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbId)
                setEnumName(2, status)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }
    }

    private fun antall(connection: DBConnection, query: String): Int {
        return connection.queryFirst(query) {
            setRowMapper {
                it.getInt("antall")
            }
        }
    }
}

