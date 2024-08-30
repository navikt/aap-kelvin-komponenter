package no.nav.aap.komponenter.httpklient.httpclient.request

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostRequestTest {

    @Test
    fun contenttypestring() {
        val request = PostRequest(body = "asdf")

        assertThat(request.contentType()).isEqualTo("application/json")
    }
}