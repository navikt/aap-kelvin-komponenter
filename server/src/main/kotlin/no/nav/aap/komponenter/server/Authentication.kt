package no.nav.aap.komponenter.server

import io.ktor.client.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import no.nav.aap.komponenter.server.auth.IdentityProvider
import no.nav.aap.komponenter.server.auth.TexasAuthenticationProvider

public const val AZURE: String = "azure"
public const val TOKENX: String = "tokenx"

// FIXME: Når det er laget en variant av [commonKtorModule] som støtter nav.no-apper, kan denne gjøres intern igjen
/**
 * Installerer Authentication med TexasAuthenticationProvider for validering av token.
 * [Texas](https://docs.nais.io/auth/explanations/#texas)
 **/
public fun Application.authentication(identityProviders: List<IdentityProvider>, texasHttpClient: HttpClient? = null) {
    install(Authentication) {
        require(identityProviders.isNotEmpty()) {
            "Må ha minst en identityProvider for å kunne installere TexasAuthenticationProvider"
        }

        identityProviders.forEach { provider ->
            register(TexasAuthenticationProvider.Config(provider, texasHttpClient).build())
        }
    }
}
