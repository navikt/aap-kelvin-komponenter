package no.nav.aap.komponenter.verdityper

/** representerer en kvantitet i enhet dagsatser */
public data class Dagsatser(
    val antall: Int
) {
    public operator fun times(other: Int): Dagsatser {
        return Dagsatser(antall * other)
    }
}