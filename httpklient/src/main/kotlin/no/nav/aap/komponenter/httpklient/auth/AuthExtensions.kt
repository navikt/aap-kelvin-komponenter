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

public fun ApplicationCall.token(): OidcToken {
    val token: String = requireNotNull(this.request.headers[HttpHeaders.Authorization]).split(" ")[1]

    return OidcToken(token)
}

public fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.token(): OidcToken {
    return pipeline.context.token()
}

public fun <TResponse> OpenAPIPipelineResponseContext<TResponse>.bruker(): Bruker {
    return pipeline.context.bruker()
}
