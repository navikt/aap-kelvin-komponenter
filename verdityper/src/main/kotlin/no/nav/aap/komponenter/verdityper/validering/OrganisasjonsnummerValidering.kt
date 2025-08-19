package no.nav.aap.komponenter.verdityper.validering

public object OrganisasjonsnummerValidering {
    // Regex for å sikre at verdien består av 9 siffer og ikke er 9 av tallet 0 (eks. 000000000)
    private val regex = Regex("^(?!0{9})\\d{9}$")
    private val kontrollsiffer = intArrayOf(3, 2, 7, 6, 5, 4, 3, 2)

    public fun erGyldig(value: String): Boolean =
        regex.matches(value) &&
            validerKontrollsiffer(value)

    /**
     * Valider kontrollsiffer
     */
    private fun validerKontrollsiffer(value: String): Boolean {
        return value[8].digitToInt() == mod(value)
    }

    /**
     * Kontrollsiffer:
     *  k1 = 11 - ((3 × d1 + 7 × d2 + 6 × m1 + 1 × m2 + 8 × å1 + 9 × å2 + 4 × i1 + 5 × i2 + 2 × i3) mod 11)
     */
    private fun mod(value: String): Int {
        val sum =
            kontrollsiffer
                .withIndex()
                .sumOf { (i, m) -> m * value[i].digitToInt() }

        val result = 11 - (sum % 11)
        return if (result == 11) 0 else result
    }
}