import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.json.WhiteSpaceRemovalDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class TestCustomDeserializer {
    @Test
    fun `kan tillate whitespace på slutten av ident`() {
        val s = """{"identifikator": "12345678901 "}"""

        val ident = DefaultJsonMapper.fromJson(s, Ident::class.java)

        assertThat(ident.identifikator).isEqualTo("12345678901")
    }
}

data class Ident(@param:JsonDeserialize(using = WhiteSpaceRemovalDeserializer::class) val identifikator: String) {
    init {
        require(identifikator.matches("\\d{11}".toRegex())) { "Ugyldig identifikator. Lengden må være 11 siffer. Lengde: ${identifikator.length}. Ikke-siffer: ${identifikator.filterNot { it.isDigit() }.length}." }
    }
}