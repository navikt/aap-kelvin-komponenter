package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

interface TokenProvider {

    fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}
