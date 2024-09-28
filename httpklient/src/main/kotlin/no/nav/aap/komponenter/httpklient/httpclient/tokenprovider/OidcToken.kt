package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.auth0.jwt.JWT
import java.time.LocalDateTime
import java.time.ZoneId

private const val OID = "oid"
private const val IDTYP = "idtyp"
private const val APP = "app"

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

    public fun isClientCredentials(): Boolean {
        val subject = accessToken.subject

        // Sjekker både gammel konvensjon (oid=sub) og nyere (idtyp="app")
        return subject == accessToken.getClaim(OID).asString() ||
                APP == accessToken.getClaim(IDTYP).asString()
    }
}