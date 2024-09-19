package no.nav.aap.komponenter.miljo

import no.nav.aap.komponenter.config.configForKey

internal val ENV_VAR_NAME = "NAIS_CLUSTER_NAME"

public object Miljø {
    public fun er(): MiljøKode {
        val cluster = configForKey(ENV_VAR_NAME)
        if (cluster == "LOCAL") {
            return MiljøKode.LOKALT
        } else if (cluster?.substring(0, cluster.indexOf("-"))?.equals("DEV", ignoreCase = true) == true) {
            return MiljøKode.DEV
        }
        return MiljøKode.PROD
    }
}