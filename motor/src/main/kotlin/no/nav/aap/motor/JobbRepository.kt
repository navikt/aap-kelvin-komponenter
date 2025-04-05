package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

internal class JobbRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(JobbRepository::class.java)

    fun leggTil(jobbInput: JobbInput) {
        val oppgaveId = connection.executeReturnKey(
            """
            INSERT INTO JOBB 
            (sak_id, behandling_id, type, neste_kjoring, parameters, payload) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.sakIdOrNull())
                setLong(2, jobbInput.behandlingIdOrNull())
                setString(3, jobbInput.type())
                setLocalDateTime(4, jobbInput.nesteKjøringTidspunkt())
                setProperties(5, jobbInput.properties)
                setString(6, jobbInput.payload)
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, oppgaveId)
                setEnumName(2, JobbStatus.KLAR)
            }
        }
        log.info("Planlagt kjøring av jobb[${jobbInput.type()}] med kjøring etter ${jobbInput.nesteKjøringTidspunkt()}")
    }

    internal fun plukkJobb(): JobbInput? {

        val query = """
            with ekskluderende_jobb as (
                select distinct on (sak_id, behandling_id) id, status, neste_kjoring
                from jobb
                where status IN ('${JobbStatus.FEILET.name}', '${JobbStatus.KLAR.name}')
                  and (sak_id is not null or (sak_id is null and behandling_id is not null))
                  and neste_kjoring < ?
                order by sak_id, behandling_id, neste_kjoring asc
            ),
            klar_ekskluderende_jobb as (
                select id, neste_kjoring
                from ekskluderende_jobb
                where status = '${JobbStatus.KLAR.name}'
            ),
            klar_selvstendig_jobb as (
                select id, neste_kjoring
                from jobb
                where status = '${JobbStatus.KLAR.name}'
                  and sak_id is null
                  and behandling_id is null
                  and neste_kjoring < ?
            ),
            jobb_kandidat as (
                (select * from klar_ekskluderende_jobb)
                union
                (select * from klar_selvstendig_jobb)
            )

            select jobb.id,
                   jobb.type,
                   jobb.status,
                   jobb.sak_id,
                   jobb.behandling_id,
                   jobb.neste_kjoring,
                   jobb.parameters,
                   jobb.payload,
                   (select count(1)
                    from jobb_historikk
                    where jobb_historikk.jobb_id = jobb.id
                      and jobb_historikk.status = '${JobbStatus.FEILET.name}') as antall_feil
            from jobb
            inner join jobb_kandidat on jobb_kandidat.id = jobb.id
            order by jobb_kandidat.neste_kjoring asc
            for update skip locked
            limit 1
        """.trimIndent()

        val plukketJobb = connection.queryFirstOrNull(query) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
                setLocalDateTime(2, LocalDateTime.now())
            }
            setRowMapper { row ->
                JobbInputParser.mapJobb(row)
            }
        }

        if (plukketJobb == null) {
            return null
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, plukketJobb.id)
                setEnumName(2, JobbStatus.PLUKKET)
            }
        }

        return plukketJobb
    }

    internal fun markerKjørt(jobbInput: JobbInput) {
        connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = 'KLAR'") {
            setParams {
                setEnumName(1, JobbStatus.FERDIG)
                setLong(2, jobbInput.id)
            }
            setResultValidator {
                require(it == 1)
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status) VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.id)
                setEnumName(2, JobbStatus.FERDIG)
            }
        }
    }

    internal fun markerFeilet(jobbInput: JobbInput, exception: Throwable) {
        // Da denne kjører i en ny transaksjon bør oppgaven låses slik at den ikke plukkes på nytt mens det logges
        connection.queryFirst("SELECT id FROM JOBB WHERE id = ? FOR UPDATE") {
            setParams {
                setLong(1, jobbInput.id)
            }
            setRowMapper {
                it.getLong("id")
            }
        }

        if (jobbInput.skalMarkeresSomFeilet()) {
            connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = 'KLAR'") {
                setParams {
                    setEnumName(1, JobbStatus.FEILET)
                    setLong(2, jobbInput.id)
                }
                setResultValidator {
                    require(it == 1)
                }
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status, feilmelding) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.id)
                setEnumName(2, JobbStatus.FEILET)
                setString(3, exception.stackTraceToString())
            }
        }
    }

    internal fun settNesteKjøring(jobbId: Long, tidspunkt: LocalDateTime): Int {
        return connection.executeReturnUpdated("UPDATE JOBB SET neste_kjoring = ? WHERE id = ? AND status = 'KLAR'") {
            setParams {
                setLocalDateTime(1, tidspunkt)
                setLong(2, jobbId)
            }
            setResultValidator { require(it <= 1) }
        }
    }
}
