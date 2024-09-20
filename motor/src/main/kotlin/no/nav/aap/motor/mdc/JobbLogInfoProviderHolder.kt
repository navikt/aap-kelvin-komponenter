package no.nav.aap.motor.mdc

internal object JobbLogInfoProviderHolder {

    private var infoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider

    fun set(provider: JobbLogInfoProvider) {
        infoProvider = provider
    }

    fun get(): JobbLogInfoProvider {
        return infoProvider
    }
}