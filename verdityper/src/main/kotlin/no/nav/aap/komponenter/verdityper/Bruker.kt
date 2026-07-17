package no.nav.aap.komponenter.verdityper

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

/** Representerer en Nav-ansatt (eller en annen person som er logget
 * inn i Navs systemer på samme måte som ansatte).
 *
 * Se [PersonBruker] for tilsvarende klasse som representerer en privat
 * person (med fødselsnummer, d-nummer, o.l.).
 */
public data class Bruker
@JsonCreator constructor(
    /** Ident til innlogget bruker. Typisk på formet X123456. */
    @JsonValue
    public val ident: String,
) {

    /** Er det en ekte person, med Nav-ident?
     *
     * Klassen brukes også for systembrukere (`Kelvin` f.eks.).
     */
    public fun erNavIdent(): Boolean {
        return this.ident.matches(Regex("\\w\\d{6}"))
    }
}
