package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

public class NoTokenTokenProvider : TokenProvider {
    override fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}