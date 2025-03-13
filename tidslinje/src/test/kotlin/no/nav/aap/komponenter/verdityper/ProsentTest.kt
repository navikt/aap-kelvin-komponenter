package no.nav.aap.komponenter.verdityper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ProsentTest {
    @Test
    fun `gange prosent med prosent reduserer prosenten prosentvis`() {
        val initial = Prosent(80)

        val res = initial.multiplisert(Prosent(50))

        assertThat(res).isEqualTo(
            Prosent(40)
        )
    }
}