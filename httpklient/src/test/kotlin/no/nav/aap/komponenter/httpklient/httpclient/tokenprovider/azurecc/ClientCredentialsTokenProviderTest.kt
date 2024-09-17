package no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class ClientCredentialsTokenProviderTest {
    @Test
    fun `skal generer tidspunkt frem i tid på utløpstidspunkt`() {

        val expiresTime = calculateExpiresTime(3600)
        val inTheFuture = LocalDateTime.now().plusSeconds(3550)
        val evenMoreinTheFuture = LocalDateTime.now().plusSeconds(3600)

        assertThat(inTheFuture).isBefore(expiresTime)
        assertThat(evenMoreinTheFuture).isAfter(expiresTime)
    }
}

internal fun calculateExpiresTime(expiresInSec: Int): LocalDateTime {
    val expiresIn =
        Duration.ofSeconds(expiresInSec.toLong()).minus(Duration.ofSeconds(30))

    return LocalDateTime.now().plus(expiresIn);
}