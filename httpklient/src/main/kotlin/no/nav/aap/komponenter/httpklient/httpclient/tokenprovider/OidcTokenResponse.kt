package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
internal class OidcTokenResponse(
    @param:JsonProperty("access_token") val access_token: String,
    @param:JsonProperty("token_type") val token_type: String,
    @param:JsonProperty("scope") val scope: String?,
    @param:JsonProperty("expires_in") val expires_in: Int
) {
    override fun toString(): String {
        return "OidcTokenResponse{" + "token_type='" + token_type + ", scope='" + (scope
            ?: "") + ", expires_in=" + expires_in + '}'
    }
}