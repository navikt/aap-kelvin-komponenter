package no.nav.aap.komponenter.featuretoggle

import no.nav.aap.komponenter.config.configForKey
import java.util.HashMap

public object FeatureToggle {

    private val toggles = HashMap<String, Boolean>()

    public fun erAktivert(key: String, default: Boolean): Boolean {
        if (toggles.containsKey(key)) {
            return toggles.getValue(key)
        }

        val configForKey = configForKey(key)
        if (configForKey != null) {
            return configForKey.toBoolean()
        }
        return default
    }

    public fun aktiver(key: String, value: Boolean) {
        toggles[key] = value
    }
}