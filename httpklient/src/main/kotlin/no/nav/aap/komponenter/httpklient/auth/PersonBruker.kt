package no.nav.aap.komponenter.httpklient.auth

@Deprecated(
    "Bruk klasse i server-modulen.",
    replaceWith = ReplaceWith("PersonBruker", "no.nav.aap.komponenter.verdityper.PersonBruker")
)
public class PersonBruker(
    public val pid: String,
)