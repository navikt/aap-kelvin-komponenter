package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.auth0.jwt.JWT
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Mer info om tilgjengelige claims i token:
 * https://docs.nais.io/auth/entra-id/reference/#claims
 **/
private const val OID = "oid"
private const val IDTYP = "idtyp"
private const val APP = "app"
private const val AZP_NAME = "azp_name"
private const val NAVident = "NAVident"

public class OidcToken(accessToken: String) {
    private val accessToken = JWT.decode(accessToken)

    public fun token(): String {
        return accessToken.token
    }

    public fun expires(): LocalDateTime {
        return LocalDateTime.ofInstant(accessToken.expiresAt.toInstant(), ZoneId.systemDefault())
    }

    public fun isNotExpired(): Boolean {
        val now = LocalDateTime.now().plusSeconds(30)
        return now.isBefore(expires())
    }

    /**
     * Sjekker om token er et systembruker-token (client credentials)
     **/
    public fun isClientCredentials(): Boolean {
        val subject = accessToken.subject

        // Sjekker både gammel konvensjon (oid=sub) og nyere (idtyp="app")
        return subject == accessToken.getClaim(OID).asString() ||
                APP == accessToken.getClaim(IDTYP).asString()
    }

    /**
     * Returnerer azp_name for systembruker-token
     * Er i formatet env:namespace:app (eksempelvis dev-gcp:team:aap-komponenter)
     *
     * @throws IllegalStateException hvis token tilhører personbruker
     **/
    public fun azpName(): String {
        return if (isClientCredentials()) accessToken.getClaim(AZP_NAME).asString()
        else error("Kan kun hente azp_name for systembruker")
    }

    /**
     * Returnerer NAVident (Z-ident) for personbruker-token
     *
     * @throws IllegalStateException hvis token tilhører systembruker
     **/
    public fun navIdent(): String {
        return if (!isClientCredentials()) accessToken.getClaim(NAVident).asString()
        else error("Kan kun hente NAVident for personbruker")
    }
}