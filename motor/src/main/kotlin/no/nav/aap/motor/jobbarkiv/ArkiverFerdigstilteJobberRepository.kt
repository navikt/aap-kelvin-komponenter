package no.nav.aap.motor.jobbarkiv

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbStatus
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class ArkiverFerdigstilteJobberRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(ArkiverFerdigstilteJobberRepository::class.java)

    internal fun arkiverFerdigstilteJobber(batchStørrelse: Int): Int {
        if (!arkivtabellerFinnes()) {
            log.info("Kan ikke gjennomføre arkivering: mangler tabellene jobb_arkiv og/eller jobb_historikk_arkiv")
            return 0
        }

        val cutoff = LocalDateTime.now().minusDays(DAGER_FOR_ARKIVERING)

        // Hent liste over ID-er som skal arkiveres
        val jobberSomSkalArkiveres = connection.queryList(
            """
            SELECT id
            FROM JOBB
            WHERE status = ?
              AND neste_kjoring < ?
            LIMIT ?
            """.trimIndent()
        ) {
            setParams {
                setEnumName(1, JobbStatus.FERDIG)
                setLocalDateTime(2, cutoff)
                setInt(3, batchStørrelse)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        if (jobberSomSkalArkiveres.isEmpty()) {
            return 0
        }

        // 1. Flytt JOBB-rader til arkiv
        connection.execute(
            """
            INSERT INTO jobb_arkiv
                (id, status, type, sak_id, behandling_id, parameters, payload, neste_kjoring, opprettet_tid)
            SELECT id, status, type, sak_id, behandling_id, parameters, payload, neste_kjoring, opprettet_tid
            FROM JOBB
            WHERE id = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, jobberSomSkalArkiveres)
            }
        }

        // 2. Flytt JOBB_HISTORIKK-rader til arkiv
        connection.execute(
            """
            INSERT INTO jobb_historikk_arkiv
                (id, jobb_id, status, feilmelding, opprettet_tid)
            SELECT id, jobb_id, status, feilmelding, opprettet_tid
            FROM JOBB_HISTORIKK
            WHERE jobb_id = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, jobberSomSkalArkiveres)
            }
        }

        // 3. Slett fra JOBB_HISTORIKK
        connection.execute(
            """
            DELETE FROM JOBB_HISTORIKK
            WHERE jobb_id = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, jobberSomSkalArkiveres)
            }
        }

        // 4. Slett fra JOBB
        connection.execute(
            """
            DELETE FROM JOBB
            WHERE id = ANY(?::bigint[])
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, jobberSomSkalArkiveres)
            }
        }

        connection.markerSavepoint()
        return jobberSomSkalArkiveres.size
    }

    internal fun arkivtabellerFinnes(): Boolean {
        val query = """
            SELECT
                to_regclass('public.jobb_arkiv') IS NOT NULL AS jobb_arkiv_finnes,
                to_regclass('public.jobb_historikk_arkiv') IS NOT NULL AS jobb_historikk_arkiv_finnes
        """.trimIndent()

        return connection.queryFirst(query) {
            setRowMapper {
                it.getBoolean("jobb_arkiv_finnes") && it.getBoolean("jobb_historikk_arkiv_finnes")
            }
        }
    }

    private companion object {
        const val DAGER_FOR_ARKIVERING = 60L
    }

}
