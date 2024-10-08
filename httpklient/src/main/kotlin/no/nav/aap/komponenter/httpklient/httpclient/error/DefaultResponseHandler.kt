package no.nav.aap.komponenter.httpklient.httpclient.error

import no.nav.aap.komponenter.httpklient.httpclient.håndterStatus
import java.io.InputStream
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Håndterer å lese om en InputStream til eget format via `mapper` i [håndter]-metoden.
 * @throws IkkeFunnetException For 404-responser.
 * @throws ManglerTilgangException For 403-responser.
 */
public class DefaultResponseHandler() : RestResponseHandler<InputStream> {
    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<InputStream>,
        mapper: (InputStream, HttpHeaders) -> R
    ): R? {
        return håndterStatus(response, block = {
            val value = response.body()
            if (value == null) {
                return@håndterStatus null
            } else {
                return@håndterStatus mapper(value, response.headers())
            }
        })
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<InputStream> {
        return HttpResponse.BodyHandlers.ofInputStream()
    }
}