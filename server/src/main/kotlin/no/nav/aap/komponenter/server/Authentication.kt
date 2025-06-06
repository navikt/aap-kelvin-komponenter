package no.nav.aap.komponenter.server

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureConfig
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.tokenx.TokenxConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit

private val SECURE_LOGGER: Logger = LoggerFactory.getLogger("secureLog")

public const val AZURE: String = "azure"
public const val TOKENX: String = "tokenx"

internal fun Application.authentication(azureConfig: AzureConfig?, tokenxConfig: TokenxConfig?) {
    val configs = listOfNotNull(azureConfig?.let { AZURE to it }, tokenxConfig?.let { TOKENX to it })

    authentication {
        for ((name, config) in configs) {

            val jwkProvider: JwkProvider = JwkProviderBuilder(URI.create(config.jwksUri).toURL())
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build()

            jwt(name) {
                verifier(jwkProvider, config.issuer)
                challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, "$name validering feilet") }
                validate { cred ->
                    val now = Date()

                    if (config.clientId !in cred.audience) {
                        SECURE_LOGGER.warn("$name validering feilet (clientId var ikke i audience: ${cred.audience}")
                        return@validate null
                    }

                    if (cred.expiresAt?.before(now) == true) {
                        SECURE_LOGGER.warn("$name validering feilet (expired at: ${cred.expiresAt})")
                        return@validate null
                    }

                    if (cred.notBefore?.after(now) == true) {
                        SECURE_LOGGER.warn("$name validering feilet (not valid yet, valid from: ${cred.notBefore})")
                        return@validate null
                    }

                    if (cred.issuedAt?.after(cred.expiresAt ?: return@validate null) == true) {
                        SECURE_LOGGER.warn("$name validering feilet (issued after expiration: ${cred.issuedAt} )")
                        return@validate null
                    }

                    JWTPrincipal(cred.payload)
                }
            }
        }
    }
}
