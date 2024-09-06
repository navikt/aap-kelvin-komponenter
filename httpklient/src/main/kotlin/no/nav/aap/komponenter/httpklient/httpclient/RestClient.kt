package no.nav.aap.komponenter.httpklient.httpclient

import no.nav.aap.komponenter.httpklient.httpclient.error.DefaultResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.error.RestResponseHandler
import no.nav.aap.komponenter.httpklient.httpclient.request.BodyConverter
import no.nav.aap.komponenter.httpklient.httpclient.request.DeleteRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.Request
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.TokenProvider
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import org.slf4j.MDC
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.util.*

class RestClient<K>(
    private val config: ClientConfig,
    private val tokenProvider: TokenProvider,
    private val responseHandler: RestResponseHandler<K>,
    httpClient: HttpClient? = null
) {

    companion object {
        fun withDefaultResponseHandler(config: ClientConfig, tokenProvider: TokenProvider): RestClient<InputStream> {
            return RestClient(config, tokenProvider, DefaultResponseHandler())
        }
    }

    private val client = httpClient ?: HttpClient.newBuilder().connectTimeout(config.connectionTimeout)
        .proxy(HttpClient.Builder.NO_PROXY).followRedirects(HttpClient.Redirect.NEVER).build()

    fun <T : Any, R> post(uri: URI, request: PostRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    fun <T : Any, R> put(uri: URI, request: PutRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    fun <T : Any, R> patch(uri: URI, request: PatchRequest<T>, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    fun <R> get(uri: URI, request: GetRequest, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    fun <R> delete(uri: URI, request: DeleteRequest, mapper: (K, HttpHeaders) -> R): R? {
        val httpRequest = buildRequest(uri, request)

        return executeRequestAndHandleResponse(httpRequest, mapper)
    }

    private fun buildRequest(uri: URI, request: Request): HttpRequest {
        val (metod, bodyPublisher) = utledMethod(request)
        val httpRequest =
            HttpRequest.newBuilder(uri)
                .addHeaders(request)
                .addHeaders(config, tokenProvider, request.currentToken())
                .timeout(request.timeout()).method(metod, bodyPublisher).build()

        return httpRequest
    }

    private fun utledMethod(request: Request): Pair<String, BodyPublisher> {
        return when (request) {
            is GetRequest -> Pair("GET", HttpRequest.BodyPublishers.noBody())
            is DeleteRequest -> Pair("DELETE", HttpRequest.BodyPublishers.noBody())
            is PatchRequest<*> -> Pair(
                "PATCH",
                HttpRequest.BodyPublishers.ofString(BodyConverter.convert(request.body(), request.contentType()))
            )

            is PostRequest<*> -> Pair(
                "POST",
                HttpRequest.BodyPublishers.ofString(BodyConverter.convert(request.body(), request.contentType()))
            )

            is PutRequest<*> -> Pair(
                "PUT", HttpRequest.BodyPublishers.ofString(BodyConverter.convert(request.body(), request.contentType()))
            )
        }
    }

    private fun <R> executeRequestAndHandleResponse(request: HttpRequest, mapper: (K, HttpHeaders) -> R): R? {
        val response = client.send(request, responseHandler.bodyHandler())
        return responseHandler.håndter(request, response, mapper)
    }
}

inline fun <reified R> RestClient<InputStream>.get(uri: URI, request: GetRequest): R? {
    return get(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

inline fun <T : Any, reified R> RestClient<InputStream>.post(uri: URI, request: PostRequest<T>): R? {
    return post(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

inline fun <T : Any, reified R> RestClient<InputStream>.put(uri: URI, request: PutRequest<T>): R? {
    return put(uri, request) { body, _ -> DefaultJsonMapper.fromJson(body) }
}

private fun HttpRequest.Builder.addHeaders(restRequest: Request): HttpRequest.Builder {
    this.header("Content-Type", restRequest.contentType().toString())
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

fun sikreCorrelationId(): String {
    var callid = MDC.get("callId")
    if (callid == null) {
        val uuid = UUID.randomUUID()
        callid = uuid.toString()
        MDC.put("callId", callid)
    }
    return callid
}
