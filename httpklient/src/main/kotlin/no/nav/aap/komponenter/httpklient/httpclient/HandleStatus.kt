package no.nav.aap.komponenter.httpklient.httpclient

import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.ManglerTilgangException
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import java.net.HttpURLConnection
import java.net.http.HttpResponse

internal fun <E, R> håndterStatus(response: HttpResponse<E>, block: () -> R?): R? {
    val status: Int = response.statusCode()
    if (status == HttpURLConnection.HTTP_NO_CONTENT) {
        return null
    }
    if (status == HttpURLConnection.HTTP_BAD_REQUEST) {
        throw BadRequestHttpResponsException("$response :: ${response.body()}")
    }
    if (status >= HttpURLConnection.HTTP_INTERNAL_ERROR && status < HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
        throw InternalServerErrorHttpResponsException("$response :: ${response.body()}")
    }
    if (status == HttpURLConnection.HTTP_FORBIDDEN) {
        throw ManglerTilgangException("$response :: ${response.body()}")
    }

    if ((status >= HttpURLConnection.HTTP_OK && status < HttpURLConnection.HTTP_MULT_CHOICE)) {
        return block()
    }

    if (status == HttpURLConnection.HTTP_NOT_FOUND) {
        throw IkkeFunnetException("$response :: ${response.body()}")
    }

    throw UhåndtertHttpResponsException("Uventet HTTP-responskode $response")
}