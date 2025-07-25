package no.nav.aap.komponenter.httpklient.auth

@Deprecated(
    "Bruk klasse i server-modulen i stedet.",
    replaceWith = ReplaceWith("Bruker", "no.nav.aap.komponenter.verdityper.Bruker")
)
public data class Bruker(public val ident: String)
