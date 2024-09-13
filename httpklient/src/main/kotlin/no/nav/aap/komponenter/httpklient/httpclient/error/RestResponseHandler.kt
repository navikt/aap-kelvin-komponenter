package no.nav.aap.komponenter.httpklient.httpclient.error

import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

public interface RestResponseHandler<K> {

    public fun <R> håndter(request: HttpRequest, response: HttpResponse<K>, mapper: (K, HttpHeaders) -> R) : R?

    public fun bodyHandler(): HttpResponse.BodyHandler<K>
}