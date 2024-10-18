package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection

public class FlytJobbRepository(private val connection: DBConnection) {
    private val jobbRepository = JobbRepository(connection)

    public fun leggTil(jobbInput: JobbInput) {
        jobbRepository.leggTil(jobbInput)
    }

    public fun hentJobberForBehandling(id: Long): List<JobbInput> {
        val query = """
            SELECT *, (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = op.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
                 FROM JOBB op
                 WHERE op.status IN ('${JobbStatus.KLAR.name}','${JobbStatus.FEILET.name}')
                   AND op.behandling_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                JobbInputParser.mapJobb(row)
            }
        }
    }

    public fun hentFeilmeldingForOppgave(id: Long): String {
        val query = """
            SELECT * 
            FROM JOBB_HISTORIKK 
            WHERE jobb_id = ? and status = '${JobbStatus.FEILET.name}'
            ORDER BY OPPRETTET_TID DESC
            LIMIT 1
        """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                row.getString("feilmelding")
            }
        }
    }
}
