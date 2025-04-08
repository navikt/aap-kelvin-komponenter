package no.nav.aap.komponenter.httpklient.exception

import io.ktor.http.*

public enum class ApiErrorCode {
    INTERNFEIL,
    UKJENT_FEIL,
    IKKE_FUNNET,
    ENDEPUNKT_IKKE_FUNNET,
    UGYLDIG_FORESPØRSEL,
}

/**
 * TODO:
 *  Bytte ut alt under [no.nav.aap.komponenter.httpklient.httpclient.error] med error av type [ApiException]
 */
public open class ApiException(
    public open val status: HttpStatusCode,
    public open val code: ApiErrorCode? = null,
    public override val message: String,
    public override val cause: Throwable? = null,
) : Exception(message, cause) {
    public fun tilApiErrorResponse(): ApiErrorRespons = ApiErrorRespons(
        message = message,
        code = code?.name ?: ApiErrorCode.UKJENT_FEIL.name
    )
}

public class VerdiIkkeFunnetException(
    override val message: String,
    override val code: ApiErrorCode? = null,
) : ApiException(
    status = HttpStatusCode.NotFound,
    message = message,
    code = code ?: ApiErrorCode.IKKE_FUNNET
)

public class IkkeTillattException(
    override val message: String
) : ApiException(
    status = HttpStatusCode.Forbidden,
    message = message
)

public class UgyldigForespørselException(
    override val message: String,
    override val code: ApiErrorCode? = null,
    override val cause: Throwable? = null,
) : ApiException(
    status = HttpStatusCode.BadRequest,
    code = code ?: ApiErrorCode.UGYLDIG_FORESPØRSEL,
    message = message,
    cause = cause
)

public class InternfeilException(
    override val message: String,
    override val cause: Throwable? = null,
) : ApiException(
    status = HttpStatusCode.InternalServerError,
    code = ApiErrorCode.INTERNFEIL,
    message = message,
    cause = cause
)