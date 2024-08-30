package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
internal class OidcTokenResponse(
    @JsonProperty("access_token") val access_token: String,
    @JsonProperty("token_type") val token_type: String,
    @JsonProperty("scope") val scope: String?,
    @JsonProperty("expires_in") val expires_in: Int
) {
    override fun toString(): String {
        return "OidcTokenResponse{" + "token_type='" + token_type + ", scope='" + (scope
            ?: "") + ", expires_in=" + expires_in + '}'
    }
}