package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import java.net.URI

@Deprecated("Fases ut siden all autentisering burde gå via Texas")
public interface JwtConfig {
    public val tokenEndpoint: URI
    public val clientId: String
    public val clientSecret: String
    public val jwksUri: String
    public val issuer: String
}