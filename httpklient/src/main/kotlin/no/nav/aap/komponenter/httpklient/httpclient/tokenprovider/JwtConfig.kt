package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import java.net.URI

public interface JwtConfig {
    public val tokenEndpoint: URI
    public val clientId: String
    public val clientSecret: String
    public val jwksUri: String
    public val issuer: String
}