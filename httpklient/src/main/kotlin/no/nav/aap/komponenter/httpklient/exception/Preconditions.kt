@file:OptIn(ExperimentalContracts::class)

package no.nav.aap.komponenter.httpklient.exception

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Erstatter Kotlin sin precondition [require]
 *
 * @throws InternfeilException med [feilmelding] dersom [verdi] er false
 **/
public inline fun krev(
    verdi: Boolean,
    feilmelding: () -> String,
) {
    contract {
        returns() implies verdi
    }

    if (!verdi) {
        throw InternfeilException(feilmelding())
    }
}

/**
 * Erstatter Kotlin sin precondition [requireNotNull]
 *
 * @throws InternfeilException med [feilmelding] dersom [verdi] er null
 *
 * @return [verdi] dersom den ikke er null
 **/
public inline fun <T : Any> krevIkkeNull(
    verdi: T?,
    feilmelding: () -> String,
): T {
    contract {
        returns() implies (verdi != null)
    }

    return verdi ?: throw InternfeilException(feilmelding())
}

/**
 * Erstatter Kotlin sin precondition [check]
 *
 * @throws UgyldigForespørselException med [feilmelding] dersom [verdi] er false
 **/
public inline fun sjekk(
    verdi: Boolean,
    feilmelding: () -> String,
) {
    contract {
        returns() implies verdi
    }

    if (!verdi) {
        throw UgyldigForespørselException(message = feilmelding())
    }
}

/**
 * Erstatter Kotlin sin precondition [checkNotNull]
 *
 * @throws UgyldigForespørselException med [feilmelding] dersom [verdi] er null
 *
 * @return [verdi] dersom den ikke er null
 **/
public inline fun <T : Any> sjekkIkkeNull(
    verdi: T?,
    feilmelding: () -> String,
): T {
    contract {
        returns() implies (verdi != null)
    }

    return verdi ?: throw UgyldigForespørselException(feilmelding(), ApiErrorCode.VERDI_MANGLER)
}
