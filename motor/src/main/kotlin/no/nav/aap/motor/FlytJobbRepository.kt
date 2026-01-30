package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.RepositoryFactory
import no.nav.aap.komponenter.repository.Repository

public interface FlytJobbRepository: Repository {
    public fun leggTil(jobbInput: JobbInput)
    public fun hentJobberForBehandling(id: Long): List<JobbInput>
    public fun hentJobberForSak(id: Long): List<JobbInput>
    public fun hentFeilmeldingForOppgave(id: Long): String

    public companion object {
        public operator fun invoke(connection: DBConnection): FlytJobbRepository {
            return FlytJobbRepositoryImpl(connection)
        }
    }
}

public class FlytJobbRepositoryImpl(private val connection: DBConnection) : FlytJobbRepository {
    private val jobbRepository = JobbRepository(connection)

    override fun leggTil(jobbInput: JobbInput) {
        jobbRepository.leggTil(jobbInput)
    }

    override fun hentJobberForBehandling(id: Long): List<JobbInput> {
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

    override fun hentJobberForSak(sakid: Long): List<JobbInput> {
        val query = """
            SELECT *, (SELECT count(1) FROM JOBB_HISTORIKK h WHERE h.jobb_id = op.id AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
                 FROM JOBB op
                 WHERE op.status IN ('${JobbStatus.KLAR.name}','${JobbStatus.FEILET.name}')
                   AND op.sak_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, sakid)
            }
            setRowMapper { row ->
                JobbInputParser.mapJobb(row)
            }
        }
    }

    override fun hentFeilmeldingForOppgave(id: Long): String {
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

    public companion object: RepositoryFactory<FlytJobbRepository> {
        override fun konstruer(connection: DBConnection): FlytJobbRepository {
            return FlytJobbRepositoryImpl(connection)
        }
    }
}
