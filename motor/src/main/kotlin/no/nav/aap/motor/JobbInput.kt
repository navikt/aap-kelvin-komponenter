package no.nav.aap.motor

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.cron.CronExpression
import org.slf4j.MDC
import java.time.LocalDateTime
import java.util.*

private const val CALL_ID_KEY = "CallId"

public class JobbInput(
    internal val jobbSpesifikasjon: JobbSpesifikasjon,
)  {

    internal var id: Long? = null
    private var sakId: Long? = null
    private var behandlingId: Long? = null
    private var nesteKjøring: LocalDateTime? = null
    private var antallFeil: Long = 0
    private var status: JobbStatus = JobbStatus.KLAR
    internal var properties = Properties()
    internal var payload: String? = null
    internal var opprettetTidspunkt: LocalDateTime? = null

    internal fun medId(id: Long): JobbInput {
        this.id = id
        return this
    }

    internal fun medStatus(status: JobbStatus): JobbInput {
        this.status = status
        return this
    }

    internal fun medOpprettetTidspunkt(opprettetTidspunkt: LocalDateTime): JobbInput {
        this.opprettetTidspunkt = opprettetTidspunkt
        return this
    }

    /**
     * Når man trenger å gruppere jobber etter flere enn et parameter
     */
    public fun forBehandling(sakID: Long, behandlingId: Long): JobbInput {
        this.sakId = sakID
        this.behandlingId = behandlingId

        return this
    }

    internal fun gruppering(sakID: Long?, behandlingId: Long?): JobbInput {
        this.sakId = sakID
        this.behandlingId = behandlingId

        return this
    }

    /**
     * For gruppering etter en enkelt felles nøkkel
     */
    public fun forSak(sakId: Long): JobbInput {
        this.sakId = sakId

        return this
    }

    public fun medParameter(key: String, value: String): JobbInput {
        this.properties.setProperty(key, value)

        return this
    }

    public fun medPayload(payload: String?): JobbInput {
        this.payload = payload
        return this
    }

    public fun <T> medPayload(payload: T?): JobbInput {
        this.payload = payload?.let { value -> DefaultJsonMapper.toJson(value) }
        return this
    }

    public fun sakIdOrNull(): Long? {
        return sakId
    }

    public fun sakId(): Long {
        return sakId!!
    }

    public fun status(): JobbStatus {
        return status
    }

    public fun behandlingId(): Long {
        return behandlingId!!
    }

    public fun behandlingIdOrNull(): Long? {
        return behandlingId
    }

    internal fun medAntallFeil(antallFeil: Long): JobbInput {
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

    public fun type(): String {
        return jobbSpesifikasjon.type
    }

    public fun medNesteKjøring(nesteKjøring: LocalDateTime): JobbInput {
        this.nesteKjøring = nesteKjøring
        return this
    }

    public fun skalMarkeresSomFeilet(): Boolean {
        return jobbSpesifikasjon.retries <= antallFeil + 1
    }

    public fun cron(): CronExpression? {
        return jobbSpesifikasjon.cron
    }

    public fun erScheduledOppgave(): Boolean {
        return cron() != null
    }

    public fun parameter(key: String): String {
        return properties.getProperty(key)
    }

    public fun optionalParameter(key: String): String? {
        return properties.getProperty(key)
    }

    public fun harPayload(): Boolean {
        return payload != null
    }

    public inline fun <reified T> payload(): T {
        return DefaultJsonMapper.fromJson(payload()) as T
    }

    public fun payload(): String {
        return requireNotNull(payload)
    }

    override fun toString(): String {
        return "[${jobbSpesifikasjon.type}] - ${nesteKjøringTidspunkt()} - id = $id, sakId = $sakId, behandlingId = $behandlingId"
    }

    public fun medProperties(properties: Properties?): JobbInput {
        if (properties != null) {
            this.properties = properties
        }
        return this
    }

    public fun antallRetriesForsøkt(): Int {
        return antallFeil.toInt()
    }

    public fun jobbId(): Long {
        return requireNotNull(id)
    }

    public fun nesteKjøring(): LocalDateTime {
        return requireNotNull(nesteKjøring)
    }

    public fun opprettetTidspunkt(): LocalDateTime {
        return requireNotNull(opprettetTidspunkt)
    }

    public fun navn(): String {
        return jobbSpesifikasjon.navn
    }

    public fun beskrivelse(): String {
        return jobbSpesifikasjon.beskrivelse
    }

    public fun callId(): String? {
        return properties.getProperty(CALL_ID_KEY)
    }

    /**
     * Henter CallId fra MDC og viderefører denne i planlagt jobb
     */
    public fun medCallId(): JobbInput {
        val value = MDC.get("callId")
        if (value != null) {
            medParameter(CALL_ID_KEY, value)
        }
        return this
    }

}