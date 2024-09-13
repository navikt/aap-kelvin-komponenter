package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

public interface TokenProvider {

    public fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}
