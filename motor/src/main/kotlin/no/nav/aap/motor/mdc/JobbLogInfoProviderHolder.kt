package no.nav.aap.motor.mdc

public object JobbLogInfoProviderHolder {

    private var infoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider

    internal fun set(provider: JobbLogInfoProvider) {
        infoProvider = provider
    }

    public fun get(): JobbLogInfoProvider {
        return infoProvider
    }
}