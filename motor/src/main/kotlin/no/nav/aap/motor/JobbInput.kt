package no.nav.aap.motor

import no.nav.aap.motor.cron.CronExpression
import org.slf4j.MDC
import java.time.LocalDateTime
import java.util.*

private const val CALL_ID_KEY = "CallId"

class JobbInput(internal val jobb: Jobb) {

    internal var id: Long? = null
    private var sakId: Long? = null
    private var behandlingId: Long? = null
    private var nesteKjøring: LocalDateTime? = null
    private var antallFeil: Long = 0
    private var status: JobbStatus = JobbStatus.KLAR
    internal var properties = Properties()
    internal var payload: String? = null

    internal fun medId(id: Long): JobbInput {
        this.id = id
        return this
    }

    internal fun medStatus(status: JobbStatus): JobbInput {
        this.status = status
        return this
    }

    fun forBehandling(sakID: Long?, behandlingId: Long?): JobbInput {
        this.sakId = sakID
        this.behandlingId = behandlingId

        return this
    }

    fun forSak(sakId: Long): JobbInput {
        this.sakId = sakId

        return this
    }

    fun medParameter(key: String, value: String): JobbInput {
        this.properties.setProperty(key, value)

        return this
    }

    fun medPayload(payload: String?): JobbInput {
        this.payload = payload
        return this
    }

    fun sakIdOrNull(): Long? {
        return sakId
    }

    fun sakId(): Long {
        return sakId!!
    }

    fun status(): JobbStatus {
        return status
    }

    fun behandlingId(): Long {
        return behandlingId!!
    }

    fun behandlingIdOrNull(): Long? {
        return behandlingId
    }

    fun medAntallFeil(antallFeil: Long): JobbInput {
        this.antallFeil = antallFeil
        return this
    }

    internal fun nesteKjøringTidspunkt(): LocalDateTime {
        if (nesteKjøring != null) {
            return nesteKjøring as LocalDateTime
        }
        nesteKjøring = LocalDateTime.now()
        return LocalDateTime.now()
    }

    fun type(): String {
        return jobb.type()
    }

    fun medNesteKjøring(nesteKjøring: LocalDateTime): JobbInput {
        this.nesteKjøring = nesteKjøring
        return this
    }

    fun skalMarkeresSomFeilet(): Boolean {
        return jobb.retries() <= antallFeil + 1
    }

    fun cron(): CronExpression? {
        return jobb.cron()
    }

    fun erScheduledOppgave(): Boolean {
        return cron() != null
    }

    fun parameter(key: String): String {
        return properties.getProperty(key)
    }

    fun harPayload(): Boolean {
        return payload != null
    }

    fun payload(): String {
        return requireNotNull(payload)
    }

    override fun toString(): String {
        return "[${jobb.type()}] - id = $id, sakId = $sakId, behandlingId = $behandlingId"
    }

    fun medProperties(properties: Properties?): JobbInput {
        if (properties != null) {
            this.properties = properties
        }
        return this
    }

    fun antallRetriesForsøkt(): Int {
        return antallFeil.toInt()
    }

    fun jobbId(): Long {
        return requireNotNull(id)
    }

    fun nesteKjøring(): LocalDateTime {
        return requireNotNull(nesteKjøring)
    }

    fun navn(): String {
        return jobb.navn()
    }

    fun beskrivelse(): String {
        return jobb.beskrivelse()
    }

    fun callId(): String? {
        return properties.getProperty(CALL_ID_KEY)
    }

    /**
     * Henter CallId fra MDC og viderefører denne i planlagt jobb
     */
    fun medCallId(): JobbInput {
        val value = MDC.get(CALL_ID_KEY)
        if (value != null) {
            medParameter(CALL_ID_KEY, value)
        }
        return this
    }

}