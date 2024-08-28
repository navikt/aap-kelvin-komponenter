package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row

fun mapJobb(row: Row): JobbInput {
    return JobbInput(JobbType.parse(row.getString("type")))
        .medId(row.getLong("id"))
        .medStatus(row.getEnum("status"))
        .forBehandling(
            row.getLongOrNull("sak_id"),
            row.getLongOrNull("behandling_id")
        )
        .medNesteKjøring(row.getLocalDateTime("neste_kjoring"))
        .medAntallFeil(row.getLong("antall_feil"))
}

fun mapJobbInklusivFeilmelding(row: Row): Pair<JobbInput, String?> {
    return mapJobb(row) to row.getStringOrNull("feilmelding")
}

class FlytJobbRepository(private val connection: DBConnection) {
    private val jobbRepository = JobbRepository(connection)

    fun leggTil(jobbInput: JobbInput) {
        jobbRepository.leggTil(jobbInput)
    }

    fun hentJobberForBehandling(id: Long): List<JobbInput> {
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
                mapJobb(row)
            }
        }
    }

    fun hentFeilmeldingForOppgave(id: Long): String {
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