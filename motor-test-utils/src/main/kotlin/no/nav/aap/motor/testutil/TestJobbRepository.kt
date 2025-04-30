package no.nav.aap.motor.testutil

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbInputParser
import no.nav.aap.motor.JobbStatus

public class TestJobbRepository(
    private val connection: DBConnection,
    private val jobberSomSkalIgnoreres: List<String> = emptyList()
) {

    public fun harJobb(sakId: Long?, behandlingId: Long?): Boolean {

        var query = """SELECT count(1) as antall 
                FROM JOBB 
                WHERE status not in ('${JobbStatus.FERDIG.name}', '${JobbStatus.FEILET.name}')
                """.trimIndent()


        val params = java.util.HashMap<String, Long>()

        if (jobberSomSkalIgnoreres.isNotEmpty()) {
            query += " AND type != ANY(?::text[])"
        }

        if (sakId != null) {
            query += " AND sak_id = ?"
            params["sak_id"] = sakId
        }
        if (behandlingId != null) {
            query += " AND behandling_id = ?"
            params["behandling_id"] = behandlingId
        }


        var param = 1

        val antall =
            connection.queryFirst(
                query
            ) {

                setParams {
                    if (jobberSomSkalIgnoreres.isNotEmpty()) {
                        setArray(param++, jobberSomSkalIgnoreres)
                    }
                    if (params.isNotEmpty()) {
                        if (params["sak_id"] != null) {
                            setLong(param++, params["sak_id"]!!)
                        }
                        if (params["behandling_id"] != null) {
                            setLong(param++, params["behandling_id"]!!)
                        }
                    }
                }
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }

    public fun hentJobberAvTypeMedAttributter(
        type: String,
        sakId: Long?,
        behandlingId: Long?,
    ): List<JobbInput> {
        @Suppress("DEPRECATION")
        return hentJobberAvTypeMedAttributter(type, sakId, behandlingId, null)
    }

    @Deprecated("Bruk hentJobberAvTypeMedAttributter(type, sakId, behandlingId) i stedet.")
    public fun hentJobberAvTypeMedAttributter(
        type: String,
        sakId: Long?,
        behandlingId: Long?,
        property: String?
    ): List<JobbInput> {
        var query = """
            SELECT id, type, status, sak_id, behandling_id, neste_kjoring, parameters, payload, 
                (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = o.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
            FROM JOBB o
            WHERE type = ?
        """.trimIndent()

        val params = java.util.HashMap<String, Long>()

        if (sakId != null) {
            query += " AND sak_id = ?"
            params["sak_id"] = sakId
        }
        if (behandlingId != null) {
            query += " AND behandling_id = ?"
            params["behandling_id"] = behandlingId
        }

        var param = 2

        return connection.queryList(query) {
            setParams {
                setString(1, type)
                if (params.isNotEmpty()) {
                    if (params["sak_id"] != null) {
                        setLong(param++, params["sak_id"]!!)
                    }
                    if (params["behandling_id"] != null) {
                        setLong(param++, params["behandling_id"]!!)
                    }
                }
            }
            setRowMapper {
                JobbInputParser.mapJobb(it)
            }
        }
    }

}
