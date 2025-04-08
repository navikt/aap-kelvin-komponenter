package no.nav.aap.komponenter.httpklient.auth

import com.papsign.ktor.openapigen.route.response.OpenAPIPipelineResponseContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken


public fun ApplicationCall.bruker(): Bruker {
    val navIdent = principal<JWTPrincipal>()?.getClaim("NAVident", String::class)
    if (navIdent == null) {
        error("NAVident mangler i AzureAD claims")
    }
    return Bruker(navIdent)
}

public fun ApplicationCall.personBruker(): PersonBruker {
    val pid = principal<JWTPrincipal>()?.getClaim("pid", String::class)
    if (pid == null) {
        error("pid (personidentifikator) mangler i tokenx claims")
    }
    return PersonBruker(pid)

}

/**
 * Vil teste først. Mistenker at audience er på formen dev-gcp:team:app. Men returnerer string først.
 */
public fun ApplicationCall.audience(): String {
    val aud = principal<JWTPrincipal>()?.getClaim("aud", String::class)
        ?: error("aud mangler i tokenx claims")
    return aud
}

public fun ApplicationCall.groups(): List<String> {
    val groups = principal<JWTPrincipal>()?.getListClaim(
        "groups",
        String::class
    ) ?: error("groups mangler i AzureAd claims")
    return groups
}

public fun ApplicationCall.token(): OidcToken {
    val token: String = requireNotNull(this.request.headers[HttpHeaders.Authorization]).split(" ")[1]

    return OidcToken(token)
}

public fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.token(): OidcToken {
    return pipeline.call.token()
}

public fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.bruker(): Bruker {
    return pipeline.call.bruker()
}

public fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.personBruker(): PersonBruker {
    return pipeline.call.personBruker()
}
