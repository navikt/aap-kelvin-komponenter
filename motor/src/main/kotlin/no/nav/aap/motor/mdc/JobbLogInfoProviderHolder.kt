package no.nav.aap.motor.mdc

// TODO: bedre å eksponere denne via API? Den virker som implementasjonsdetalj i  Motor?
public object JobbLogInfoProviderHolder {

    private var infoProvider: JobbLogInfoProvider = NoExtraLogInfoProvider

    public fun set(provider: JobbLogInfoProvider) {
        infoProvider = provider
    }

    public fun get(): JobbLogInfoProvider {
        return infoProvider
    }
}