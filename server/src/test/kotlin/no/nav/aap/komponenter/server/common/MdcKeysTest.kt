package no.nav.aap.komponenter.server.common

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class MdcKeysTest {
    @Test
    fun `sikre at mdc-nøkler ikke endres ved uhell`() {
        Assertions.assertThat(MdcKeys.CallId).isEqualTo("callId")
        Assertions.assertThat(MdcKeys.User).isEqualTo("x_user")
        Assertions.assertThat(MdcKeys.Method).isEqualTo("x_method")
        Assertions.assertThat(MdcKeys.InboundUri).isEqualTo("x_inbound_uri")
    }
}