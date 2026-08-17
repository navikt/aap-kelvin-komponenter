package no.nav.aap.motor

/**
 * Prioritet avgjør hvilken jobb motoren plukker når flere jobber er klare samtidig.
 *
 * **Lavere verdi = høyere prioritet**, som `nice` i Unix. Grunnen til at skalaen går
 * "feil" vei er at plukkespørringen sorterer `order by prioritet, neste_kjoring` – begge
 * stigende. Da kan Postgres lese radene ferdig sortert rett ut av indeksen
 * `IDX_JOBB_PLUKK (PRIORITET, NESTE_KJORING)` uten et eget sorteringssteg.
 *
 * Verdiene ligger med luft mellom seg slik at nye nivåer kan settes inn uten migrasjon.
 * Prioritet er en vanlig `Int` og ikke en enum nettopp for å tillate mellomverdier.
 *
 * ## Prioritet settes per jobb-instans, ikke bare per type
 *
 * En jobbtype har en default via [JobbSpesifikasjon.prioritet], men den enkelte
 * innleggingen kan overstyre med [JobbInput.medPrioritet]. Det er dette som gjør at samme
 * jobbtype kan ha ulik hastverk avhengig av hvorfor den ble opprettet:
 *
 * ```kotlin
 * // Automatisk opprettet av en batch – kan vente
 * JobbInput(BrevbestillingJobb).medPrioritet(Prioritet.LAV)
 *
 * // Manuelt utløst av saksbehandler som venter på svar
 * JobbInput(BrevbestillingJobb).medPrioritet(Prioritet.KRITISK)
 * ```
 *
 * ## Viktig begrensning
 *
 * Prioritet påvirker **kun rekkefølgen mellom uavhengige jobber**. Innad i en
 * eksklusivitetsgruppe – jobber med samme `sak_id`, `behandling_id` og `type` – gjelder
 * fortsatt streng rekkefølge etter `neste_kjoring`. En høyt prioritert jobb som legges inn
 * bak en lavt prioritert jobb i samme gruppe må vente på tur. Rekkefølgegarantien er
 * sterkere enn prioritet, fordi jobber i samme gruppe kan avhenge av hverandres
 * sideeffekter. Skal du forbi køen der, er den lovlige veien å endre `neste_kjoring`.
 */
public object Prioritet {

    /** Saksbehandler eller bruker venter aktivt på resultatet. Bruk sparsomt. */
    public const val KRITISK: Int = 10

    /** Brukerinitiert arbeid som bør skje raskt, men der ingen venter foran skjermen. */
    public const val HØY: Int = 50

    /** Standard. Alt som ikke aktivt er vurdert til noe annet havner her. */
    public const val NORMAL: Int = 100

    /** Automatisk generert arbeid som tåler å ligge i kø bak brukerinitiert arbeid. */
    public const val LAV: Int = 200

    /** Vedlikehold: arkivering, opprydding, gjenoppretting. Kjører når det er ledig kapasitet. */
    public const val BAKGRUNN: Int = 500
}
