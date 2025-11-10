package no.nav.aap.motor

import no.nav.aap.komponenter.dbconnect.DBConnection
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

public class JobbRepository(private val connection: DBConnection) {
    private val log = LoggerFactory.getLogger(JobbRepository::class.java)

    public fun leggTil(jobbInput: JobbInput): Long {
        val jobbId = connection.executeReturnKey(
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
            (jobb_id, status, opprettet_tid) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbId)
                setEnumName(2, JobbStatus.KLAR)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }
        log.info("Planlagt kjøring av jobb[${jobbInput.type()}] med kjøring etter ${jobbInput.nesteKjøringTidspunkt()}. Jobb-ID: $jobbId")
        return jobbId
    }

    public fun plukkJobb(): JobbInput? {
        /** Selv om `jobb_kandidat` kun inneholder jobber med `status = 'KLAR'`, så er det ikke
         * noe som forhindrer at flere transaksjoner startet med samme snapshot og derfor
         * anser samme rad som ledig. `FOR UPDATE` er ikke tilstrekkelig for å forhindre at
         * vi returnerer en rad som en annen transaksjon har låst. Vi trenger en `WHERE` sjekk
         * sammen med `FOR UPDATE`, først da vil postgres synkronisere sjekken mot andre transaksjoner.
         * Fra dokumentasjonen om `READ COMMITED`:
         *
	     * > UPDATE, DELETE, SELECT FOR UPDATE, and SELECT FOR
	     * > SHARE commands behave the same as SELECT in terms of
	     * > searching for target rows: they will only find target
	     * > rows that were committed as of the command start time.
	     * > However, such a target row might have already been
	     * > updated (or deleted or locked) by another concurrent
	     * > transaction by the time it is found. In this case, the
	     * > would-be updater will wait for the first updating
	     * > transaction to commit or roll back (if it is still in
	     * > progress). If the first updater rolls back, then its
	     * > effects are negated and the second updater can proceed
	     * > with updating the originally found row. If the first
	     * > updater commits, the second updater will ignore the row
	     * > if the first updater deleted it, otherwise it will
	     * > attempt to apply its operation to the updated version
	     * > of the row. The search condition of the command (the
	     * > WHERE clause) is re-evaluated to see if the updated
	     * > version of the row still matches the search condition.
	     * > If so, the second updater proceeds with its operation
	     * > using the updated version of the row. In the case of
	     * > SELECT FOR UPDATE and SELECT FOR SHARE, this means it
	     * > is the updated version of the row that is locked and
	     * > returned to the client.
         */
        val query = """
            with ekskluderende_jobb as (
                select distinct on (sak_id, behandling_id, type) id, status, neste_kjoring
                from jobb
                where status IN ('${JobbStatus.FEILET.name}', '${JobbStatus.KLAR.name}')
                  and (sak_id is not null or (sak_id is null and behandling_id is not null))
                  and neste_kjoring < ?
                order by sak_id, behandling_id, type, neste_kjoring asc
            ),
            klar_ekskluderende_jobb as (
                select id, neste_kjoring
                from ekskluderende_jobb
                where status = '${JobbStatus.KLAR.name}'
                order by neste_kjoring
            ),
            klar_selvstendig_jobb as (
                select id, neste_kjoring
                from jobb
                where status = '${JobbStatus.KLAR.name}'
                  and sak_id is null
                  and behandling_id is null
                  and neste_kjoring < ?
                  order by neste_kjoring
            ),
            jobb_kandidat as (
                (select * from klar_ekskluderende_jobb)
                union all
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
                   jobb.opprettet_tid,
                   (select count(1)
                    from jobb_historikk
                    where jobb_historikk.jobb_id = jobb.id
                      and jobb_historikk.status = '${JobbStatus.FEILET.name}') as antall_feil
            from jobb
            inner join jobb_kandidat on jobb_kandidat.id = jobb.id
            where jobb.status = '${JobbStatus.KLAR.name}'
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
            (jobb_id, status, opprettet_tid) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, plukketJobb.id)
                setEnumName(2, JobbStatus.PLUKKET)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }

        return plukketJobb
    }

    public fun markerSomFerdig(jobbInput: JobbInput) {
        connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = ?") {
            setParams {
                setEnumName(1, JobbStatus.FERDIG)
                setLong(2, jobbInput.id)
                setEnumName(3, JobbStatus.KLAR)
            }
            setResultValidator {
                require(it == 1) { "Kun én jobb skal bli markert kjørt. Jobb-id: ${jobbInput.id}" }
            }
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status, opprettet_tid) VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbInput.id)
                setEnumName(2, JobbStatus.FERDIG)
                setLocalDateTime(3, LocalDateTime.now())
            }
        }
    }

    public fun markerSomFeilet(jobbInput: JobbInput, exception: Throwable) {
        // Den transaksjonen vi har gående på `connection` har allerede en lås på jobben, så vi trenger ikke ta ny lås her.

        val jobbId = jobbInput.id
        if (jobbInput.maksFeilNådd()) {
            connection.execute("UPDATE JOBB SET status = ? WHERE id = ? AND status = ?") {
                setParams {
                    setEnumName(1, JobbStatus.FEILET)
                    setLong(2, jobbId)
                    setEnumName(3, JobbStatus.KLAR)
                }
                setResultValidator {
                    require(it == 1) { "Kun én jobb skal bli markert feilet. Jobb-id: $jobbId" }
                }
            }
        }

        if (jobbInput.jobb.retryBackoffTid != null && jobbId != null) {
           val nesteKjøreTidspunkt = jobbInput.nesteKjøringTidspunkt().plus(jobbInput.jobb.retryBackoffTid)
            settNesteKjøring(jobbId, nesteKjøreTidspunkt)
        }

        connection.execute(
            """
            INSERT INTO JOBB_HISTORIKK 
            (jobb_id, status, feilmelding, opprettet_tid) VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, jobbId)
                setEnumName(2, JobbStatus.FEILET)
                setString(3, exception.stackTraceToString())
                setLocalDateTime(4, LocalDateTime.now())
            }
        }
    }

    internal fun settNesteKjøring(jobbId: Long, tidspunkt: LocalDateTime): Int {
        return connection.executeReturnUpdated("UPDATE JOBB SET neste_kjoring = ? WHERE id = ? AND status = ?") {
            setParams {
                setLocalDateTime(1, tidspunkt)
                setLong(2, jobbId)
                setEnumName(3, JobbStatus.KLAR)
            }
            setResultValidator { require(it <= 1) }
        }
    }

    public fun antallJobber(statusVerdi: JobbStatus): Long {
        val query = """
                SELECT count(*) as antall
                FROM JOBB
                WHERE status = ? AND neste_kjoring < ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setEnumName(1, statusVerdi)
                setLocalDateTime(2, LocalDateTime.now())
            }
            setRowMapper { row ->
                row.getLong("antall")
            }
        }

    }
}
