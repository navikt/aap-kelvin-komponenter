package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import no.nav.aap.komponenter.httpklient.requiredConfigForKey
import java.net.URI

class AzureConfig(
    val tokenEndpoint: URI = URI.create(requiredConfigForKey("azure.openid.config.token.endpoint")),
    val clientId: String = requiredConfigForKey("azure.app.client.id"),
    val clientSecret: String = requiredConfigForKey("azure.app.client.secret"),
    val jwksUri: String = requiredConfigForKey("azure.openid.config.jwks.uri"),
    val issuer: String = requiredConfigForKey("azure.openid.config.issuer")
)
