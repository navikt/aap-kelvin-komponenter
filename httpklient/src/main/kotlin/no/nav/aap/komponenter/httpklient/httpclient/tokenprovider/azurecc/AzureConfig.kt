package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import no.nav.aap.komponenter.config.requiredConfigForKey
import java.net.URI

public class AzureConfig(
    public val tokenEndpoint: URI = URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
    public val clientId: String = requiredConfigForKey("azure.app.client.id"),
    public val clientSecret: String = requiredConfigForKey("azure.app.client.secret"),
    public val jwksUri: String = requiredConfigForKey("azure.openid.config.jwks.uri"),
    public val issuer: String = requiredConfigForKey("azure.openid.config.issuer")
)