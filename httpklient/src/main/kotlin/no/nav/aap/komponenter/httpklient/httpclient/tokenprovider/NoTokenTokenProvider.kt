package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider

class NoTokenTokenProvider : TokenProvider {
    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}