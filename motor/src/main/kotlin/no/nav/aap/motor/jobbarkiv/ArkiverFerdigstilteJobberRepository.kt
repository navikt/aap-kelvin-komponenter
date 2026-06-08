package no.nav.aap.motor.jobbarkiv

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbStatus
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class ArkiverFerdigstilteJobberRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(ArkiverFerdigstilteJobberRepository::class.java)

    internal fun arkiverFerdigstilteJobber(): Int {
        if (!arkivtabellerFinnes()) {
            log.info("Kan ikke gjennomføre arkivering: mangler tabellene jobb_arkiv og/eller jobb_historikk_arkiv")
            return 0
        }

        val cutoff = LocalDateTime.now().minusDays(DAGER_FOR_ARKIVERING)

        val query = """
            WITH kandidater_for_arkivering AS (
                SELECT id
                FROM JOBB
                WHERE status = ?
                  AND neste_kjoring < ?
            ),
            jobber_arkivert AS (
                INSERT INTO jobb_arkiv
                    (id, status, type, sak_id, behandling_id, parameters, payload, neste_kjoring, opprettet_tid)
                SELECT j.id, j.status, j.type, j.sak_id, j.behandling_id, j.parameters, j.payload, j.neste_kjoring, j.opprettet_tid
                FROM JOBB j
                INNER JOIN kandidater_for_arkivering k ON k.id = j.id
            ),
            jobb_historikk_arkivert AS (
                INSERT INTO jobb_historikk_arkiv
                    (id, jobb_id, status, feilmelding, opprettet_tid)
                SELECT h.id, h.jobb_id, h.status, h.feilmelding, h.opprettet_tid
                FROM JOBB_HISTORIKK h
                INNER JOIN kandidater_for_arkivering k ON k.id = h.jobb_id
            ),
            jobb_historikk_slettet AS (
                DELETE FROM JOBB_HISTORIKK h
                USING kandidater_for_arkivering k
                WHERE h.jobb_id = k.id
            ),
            jobber_slettet AS (
                DELETE FROM JOBB j
                USING kandidater_for_arkivering k
                WHERE j.id = k.id
                RETURNING j.id
            )
            SELECT count(*) AS antall
            FROM jobber_slettet
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setEnumName(1, JobbStatus.FERDIG)
                setLocalDateTime(2, cutoff)
            }
            setRowMapper {
                it.getInt("antall")
            }
        }
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
