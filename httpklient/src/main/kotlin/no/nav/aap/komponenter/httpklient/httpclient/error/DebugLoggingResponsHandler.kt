package no.nav.aap.komponenter.httpklient.httpclient.error

import no.nav.aap.komponenter.httpklient.httpclient.håndterStatus
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import org.slf4j.LoggerFactory
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")

public class DebugLoggingResponsHandler : RestResponseHandler<String> {
    private val log = LoggerFactory.getLogger(DebugLoggingResponsHandler::class.java)

    override fun <R> håndter(
        request: HttpRequest,
        response: HttpResponse<String>,
        mapper: (String, HttpHeaders) -> R
    ): R? {
        return håndterStatus(
            response,
            errorBlock = {
                response.body()
            }, block = {
                val value = response.body()
                if (value == null || value.isEmpty()) {
                    null
                } else {
                    loggRespons(value)
                    mapper(value, response.headers())
                }
            })
    }

    private fun loggRespons(value: String?) {
        val miljø = Miljø.er()
        if (miljø in listOf(MiljøKode.LOKALT, MiljøKode.DEV)) {
            log.info(value)
        }
        SECURE_LOGGER.info(value)
    }

    override fun bodyHandler(): HttpResponse.BodyHandler<String> {
        return HttpResponse.BodyHandlers.ofString()
    }
}