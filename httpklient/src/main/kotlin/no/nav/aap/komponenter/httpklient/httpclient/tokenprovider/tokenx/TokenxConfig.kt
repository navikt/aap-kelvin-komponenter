package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.JwtConfig
import java.net.URI

public class TokenxConfig(
    public override val tokenEndpoint: URI = URI(requiredConfigForKey("token.x.token.endpoint")),
    public override val clientId: String = requiredConfigForKey("token.x.client.id"),
    public override val clientSecret: String = requiredConfigForKey("token.x.private.jwk"),
    public override val jwksUri: String = requiredConfigForKey("token.x.jwks.uri"),
    public override val issuer: String = requiredConfigForKey("token.x.issuer"),
) : JwtConfig
