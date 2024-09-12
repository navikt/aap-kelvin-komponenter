package no.nav.aap.komponenter.miljo

internal val ENV_VAR_NAME = "NAIS_CLUSTER_NAME"

object Miljø {
    fun er(): MiljøKode {
        val cluster = EnvironmentVariableProvider.getEnv(ENV_VAR_NAME)
        if (cluster == "LOCAL") {
            return MiljøKode.LOKALT
        } else if (cluster?.substring(0, cluster.indexOf("-"))?.equals("DEV", ignoreCase = true) == true) {
            return MiljøKode.DEV
        }
        return MiljøKode.PROD
    }
}