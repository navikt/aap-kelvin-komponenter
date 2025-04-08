package no.nav.aap.komponenter.httpklient.exception

/**
 * [ErrorRespons] skal brukes
 */
public interface ErrorRespons {
    public val message: String?
}

public data class GenerellErrorRespons(
    override val message: String?
) : ErrorRespons

public data class ApiErrorRespons(
    override val message: String,
    val code: String? = null,
) : ErrorRespons
