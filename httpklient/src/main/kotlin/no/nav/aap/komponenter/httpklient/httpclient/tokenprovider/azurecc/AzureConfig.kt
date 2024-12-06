package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.JwtConfig
import java.net.URI

public class AzureConfig(
    public override val tokenEndpoint: URI = URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
    public override val clientId: String = requiredConfigForKey("azure.app.client.id"),
    public override val clientSecret: String = requiredConfigForKey("azure.app.client.secret"),
    public override val jwksUri: String = requiredConfigForKey("azure.openid.config.jwks.uri"),
    public override val issuer: String = requiredConfigForKey("azure.openid.config.issuer")
): JwtConfig
