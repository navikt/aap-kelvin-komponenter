package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider

import com.github.benmanes.caffeine.cache.Expiry
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime

public interface TokenProvider {

    public fun getToken(scope: String?, currentToken: OidcToken?): OidcToken? {
        return null
    }
}

/** Expiry based on each token's own expiration time with a 30-second safety buffer. */
internal fun tokenExpiry() = object : Expiry<Any, OidcToken> {
    override fun expireAfterCreate(key: Any, value: OidcToken, currentTime: Long): Long {
        val remaining = Duration.between(LocalDateTime.now(), value.expires())
        return remaining.minus(Duration.ofSeconds(30)).toNanos().coerceAtLeast(0)
    }

    override fun expireAfterUpdate(key: Any, value: OidcToken, currentTime: Long, currentDuration: Long) =
        expireAfterCreate(key, value, currentTime)

    override fun expireAfterRead(key: Any, value: OidcToken, currentTime: Long, currentDuration: Long) =
        currentDuration
}

internal fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
