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
            with rekkefolge as ((select distinct on (sak_id, behandling_id) id
                                 from JOBB
                                 where status IN ('${JobbStatus.FEILET.name}', '${JobbStatus.KLAR.name}')
                                   AND (sak_id is not null OR (sak_id is null and behandling_id is not null))
                                 ORDER BY sak_id, behandling_id, neste_kjoring ASC)
                                UNION ALL
                                (select id
                                 from JOBB
                                 where status = '${JobbStatus.KLAR.name}'
                                   AND sak_id IS NULL
                                   AND BEHANDLING_id IS NULL
                                   ORDER BY neste_kjoring ASC))
                                   
            SELECT o.id,
                   o.type,
                   o.status,
                   o.sak_id,
                   o.behandling_id,
                   o.neste_kjoring,
                   o.parameters,
                   o.payload,
                   (SELECT count(1)
                    FROM JOBB_HISTORIKK h
                    WHERE h.jobb_id = o.id
                      AND h.status = '${JobbStatus.FEILET.name}') as antall_feil
            FROM JOBB o
                     INNER JOIN rekkefolge r ON r.id = o.id
            WHERE o.STATUS = '${JobbStatus.KLAR.name}'
              AND o.neste_kjoring < ?
            ORDER BY o.neste_kjoring ASC
                FOR UPDATE SKIP LOCKED
            LIMIT 1

        """.trimIndent()

        val plukketJobb = connection.queryFirstOrNull(query) {
            setParams {
                setLocalDateTime(1, LocalDateTime.now())
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
}
