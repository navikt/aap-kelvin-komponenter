package no.nav.aap.komponenter.miljo

object EnvironmentVariableProvider {

    fun getEnv(name: String): String? = System.getenv(name)

}