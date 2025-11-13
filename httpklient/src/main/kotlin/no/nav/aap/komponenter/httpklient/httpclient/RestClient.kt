package no.nav.aap.komponenter.httpklient.httpclient

import io.ktor.client.network.sockets.*
import io.ktor.client.plugins.*
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.InternalServerErrorHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.UhåndtertHttpResponsException
import no.nav.aap.komponenter.httpklient.httpclient.request.BodyConverter
import no.nav.aap.komponenter.httpklient.httpclient.request.DeleteMedBodyRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.DeleteRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.Request
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.slf4j.MDC
import java.io.InputStream
import java.net.ConnectException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpConnectTimeoutException
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpTimeoutException
import java.nio.channels.UnresolvedAddressException
import java.util.*

/**
 * @param prometheus Send inn en Prometheus-instans for å få RestClient-klassen til å generere histogrammer over
 * request-tid.
 */
public class RestClient<K>(
    private val config: ClientConfig,
    private val tokenProvider: TokenProvider,
    private val responseHandler: RestResponseHandler<K>,
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
) {
    private val log = org.slf4j.LoggerFactory.getLogger(javaClass)

    init {
        if (prometheus is SimpleMeterRegistry) {
            log.info("Send gjerne inn en ekte Prometheus-instans for å få bedre metrikker.")
        }
    }

    public companion object {
        public fun withDefaultResponseHandler(
            config: ClientConfig,
            tokenProvider: TokenProvider,
            prometheus: MeterRegistry = SimpleMeterRegistry(),
        ): RestClient<InputStream> {
            return RestClient(config, tokenProvider, DefaultResponseHandler(), prometheus)
        }
    }

    private val client = HttpClient.newBuilder().connectTimeout(config.connectionTimeout)
        .proxy(HttpClient.Builder.NO_PROXY).followRedirects(HttpClient.Redirect.NEVER).build()

    public fun <T : Any, R> post(uri: URI, request: PostRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    public fun <T : Any, R> put(uri: URI, request: PutRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    public fun <T : Any, R> patch(uri: URI, request: PatchRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    public fun <R> get(uri: URI, request: GetRequest, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    public fun <R> retryableGet(uri: URI, request: GetRequest, mapper: (K, HttpHeaders) -> R, maxRetries: Int = 2): R? {
        val httpRequest = buildRequest(uri, request)

        return retry(maxRetries) { executeRequestAndHandleResponse(httpRequest, mapper) }
    }

    public fun <T : Any, R> retryablePost(uri: URI, request: PostRequest<T>, mapper: (K, HttpHeaders) -> R, maxRetries: Int = 2): R? {
        val httpRequest = buildRequest(uri, request)

        return retry(maxRetries) { executeRequestAndHandleResponse(httpRequest, mapper) }
    }

    private fun <R> retry(retries: Int, function: () -> R?): R? {
        try {
            return function()
        } catch (ex: Exception) {
            if (retryExceptions.contains(ex::class) && retries - 1 > 0) {
                log.info("Feilet kall, retryer : {}", ex.message)
                return retry(retries - 1, function)
            } else {
                log.info("Rekjører ikke flere ganger", ex)
                throw ex
            }
        }
    }

    private val retryExceptions =
        setOf(
            SocketException::class,
            SocketTimeoutException::class,
            io.ktor.network.sockets.SocketTimeoutException::class,
            io.ktor.util.network.UnresolvedAddressException::class,
            ConnectTimeoutException::class,
            HttpRequestTimeoutException::class,
            UnresolvedAddressException::class,
            HttpTimeoutException::class,
            ConnectException::class,
            HttpConnectTimeoutException::class,
            InternalServerErrorHttpResponsException::class,
            UhåndtertHttpResponsException::class,
        )


    public fun <R> delete(uri: URI, request: DeleteRequest, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    public fun <T : Any, R> deleteMedBody(
        uri: URI,
        request: DeleteMedBodyRequest<T>,
        mapper: (K, HttpHeaders) -> R
    ): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    private fun buildRequest(uri: URI, request: Request): HttpRequest {
        val httpRequest = HttpRequest.newBuilder(uri)
            .addHeaders(request)
            .addHeaders(config, tokenProvider, request.currentToken())
            .timeout(request.timeout())

        when (request) {
            is GetRequest -> httpRequest.GET()
            is DeleteRequest -> httpRequest.DELETE()
            is PatchRequest<*> -> httpRequest.method(
                "PATCH",
                HttpRequest.BodyPublishers.ofString(BodyConverter.convert(request.body(), request.contentType()))
            ).header("Content-Type", request.contentType().toString())

            is PostRequest<*> -> httpRequest.POST(
                HttpRequest.BodyPublishers.ofString(
                    BodyConverter.convert(
                        request.body(), request.contentType()
                    )
                )
            ).header("Content-Type", request.contentType().toString())

            is DeleteMedBodyRequest<*> ->
                httpRequest.method(
                    "DELETE", HttpRequest.BodyPublishers.ofString(
                        BodyConverter.convert(
                            request.body(), request.contentType()
                        )
                    )
                )
                    .header("Content-Type", request.contentType().toString())


            is PutRequest<*> -> httpRequest.PUT(
                HttpRequest.BodyPublishers.ofString(
                    BodyConverter.convert(
                        request.body(), request.contentType()
                    )
                )
            ).header("Content-Type", request.contentType().toString())
        }

        return httpRequest.build()
    }

    private fun <R> executeRequestAndHandleResponse(request: HttpRequest, mapper: (K, HttpHeaders) -> R): R? {
        val response = client.send(request, responseHandler.bodyHandler())

        return Timer.builder("kelvin_restclient_timer")
            .tags("uri", request.uri().host, "method", request.method())
            .publishPercentileHistogram()
            .register(prometheus)
            .recordCallable<R> { responseHandler.håndter(request, response, mapper) }
    }
}

public inline fun <reified R> RestClient<InputStream>.get(uri: URI, request: GetRequest): R? {
    return get(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

public inline fun <T : Any, reified R> RestClient<InputStream>.post(uri: URI, request: PostRequest<T>): R? {
    return post(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

public inline fun <T : Any, reified R> RestClient<InputStream>.put(uri: URI, request: PutRequest<T>): R? {
    return put(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

public inline fun <T : Any, reified R> RestClient<InputStream>.patch(uri: URI, request: PatchRequest<T>): R? {
    return patch(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

private fun HttpRequest.Builder.addHeaders(restRequest: Request): HttpRequest.Builder {
    restRequest.additionalHeaders().forEach(this::addHeader)
    return this
}

private fun HttpRequest.Builder.addHeaders(
    clientConfig: ClientConfig, tokenProvider: TokenProvider, currentToken: OidcToken?
): HttpRequest.Builder {
    clientConfig.additionalHeaders.forEach(this::addHeader)
    clientConfig.additionalFunctionalHeaders.forEach(this::addHeader)
    val scope = clientConfig.scope

    val token = tokenProvider.getToken(scope, currentToken)
    if (token != null) {
        this.header("Authorization", "Bearer ${token.token()}")
    }
    val callId = sikreCorrelationId()
    this.header("X-Correlation-ID", callId)
    this.header("Nav-Call-Id", callId)
    return this
}

private fun HttpRequest.Builder.addHeader(header: Header) {
    this.header(header.key, header.value)
}

private fun HttpRequest.Builder.addHeader(header: FunctionalHeader) {
    this.header(header.key, header.supplier())
}

private fun sikreCorrelationId(): String {
    var callid = MDC.get("callId")
    if (callid == null) {
        val uuid = UUID.randomUUID()
        callid = uuid.toString()
        MDC.put("callId", callid)
    }
    return callid
}
