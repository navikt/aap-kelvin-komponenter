package no.nav.aap.komponenter.verdityper.validering

/**
 * Sjekker om en gitt folkeregisteridentifikator er gyldig.
 * Fungerer for fødselsnummer, d-nummer, og syntetiske fødselsnummere.
 **/
public object FolkeregisterIdentValidering {
    private val regex = Regex("0{11}")

    private val kontrollsiffer1 = intArrayOf(3, 7, 6, 1, 8, 9, 4, 5, 2)
    private val kontrollsiffer2 = intArrayOf(5, 4, 3, 2, 7, 6, 5, 4, 3, 2)

    /**
     * Følgende sjekkes:
     *  - Verdien består av 11 tegn
     *  - Det har en gyldig [java.math.BigInteger] verdi
     *  - Kontrollsiffer er gyldige
     */
    public fun erGyldig(verdi: String): Boolean =
        !regex.matches(verdi) &&
                verdi.length == 11 &&
                verdi.toBigIntegerOrNull() != null &&
                validerKontrollsiffer(verdi)

    /**
     * Valider kontrollsiffer
     */
    private fun validerKontrollsiffer(verdi: String): Boolean {
        val ks1 = verdi[9].digitToInt()

        val c1 = mod(kontrollsiffer1, verdi)
        if (c1 == 10 || c1 != ks1) {
            return false
        }

        val c2 = mod(kontrollsiffer2, verdi)

        return !(c2 == 10 || c2 != verdi[10].digitToInt())
    }

    /**
     * Kontrollsiffer 1:
     *  k1 = 11 - ((3 × d1 + 7 × d2 + 6 × m1 + 1 × m2 + 8 × å1 + 9 × å2 + 4 × i1 + 5 × i2 + 2 × i3) mod 11)
     *
     * Kontrolsiffer 2:
     *  k2 = 11 - ((5 × d1 + 4 × d2 + 3 × m1 + 2 × m2 + 7 × å1 + 6 × å2 + 5 × i1 + 4 × i2 + 3 × i3 + 2 × k1) mod 11)
     */
    private fun mod(arr: IntArray, verdi: String): Int {
        val sum =
            arr
                .withIndex()
                .sumOf { (i, m) -> m * verdi[i].digitToInt() }

        val result = 11 - (sum % 11)
        return if (result == 11) 0 else result
    }
}
