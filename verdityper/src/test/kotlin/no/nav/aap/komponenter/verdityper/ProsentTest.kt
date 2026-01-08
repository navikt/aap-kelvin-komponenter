package no.nav.aap.komponenter.verdityper

import no.nav.aap.komponenter.json.DefaultJsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProsentTest {
    @Test
    fun `gange prosent med prosent reduserer prosenten prosentvis`() {
        val initial = Prosent(80)

        val res = initial.multiplisert(Prosent(50))

        assertThat(res).isEqualTo(Prosent(40))
    }

    @Test
    fun `serialisere og deserialisere`() {
        val prosent = Prosent(50).multiplisert(Prosent(50))
        val json = DefaultJsonMapper.toJson(prosent)

        val deserialisertProsent = DefaultJsonMapper.fromJson<Prosent>(json)
        assertThat(deserialisertProsent).isEqualTo(prosent)
        assertThat(json).isEqualTo("25")
    }

    @Test
    fun `deserialisere liste av prosenter`() {
        val liste = """
            [30, 43]
        """.trimIndent()

        val deserialisertListe = DefaultJsonMapper.fromJson<List<Prosent>>(liste)
        assertThat(deserialisertListe).containsExactly(Prosent(30), Prosent(43))
    }
}