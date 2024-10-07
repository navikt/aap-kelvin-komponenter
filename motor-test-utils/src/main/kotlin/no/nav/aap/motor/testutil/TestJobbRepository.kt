package no.nav.aap.motor.testutil

import no.nav.aap.komponenter.dbconnect.DBConnection
import kotlin.collections.isNotEmpty

public class TestJobbRepository(private val connection: DBConnection) {
    public fun harOppgaver(sakId: Long?, behandlingId: Long?): Boolean {

        var query = "SELECT count(1) as antall " +
                "FROM JOBB " +
                "WHERE status not in ('${no.nav.aap.motor.JobbStatus.FERDIG.name}', '${no.nav.aap.motor.JobbStatus.FEILET.name}')"

        val params = java.util.HashMap<String, Long>()

        if (sakId != null) {
            query += " AND sak_id = ?"
            params["sak_id"] = sakId
        }
        if (behandlingId != null) {
            query += " AND behandling_id = ?"
            params["behandling_id"] = behandlingId
        }


        val antall =
            connection.queryFirst(
                query
            ) {
                if (params.isNotEmpty()) {
                    setParams {
                        if (params["sak_id"] != null && params["behandling_id"] == null) {
                            setLong(1, params["sak_id"]!!)
                        }
                        if (params["sak_id"] == null && params["behandling_id"] != null) {
                            setLong(1, params["behandling_id"]!!)
                        }
                        if (params["sak_id"] != null && params["behandling_id"] != null) {
                            setLong(1, params["sak_id"]!!)
                            setLong(2, params["behandling_id"]!!)
                        }
                    }
                }
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}
