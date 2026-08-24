package no.nav.aap.komponenter.json

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer


public class WhiteSpaceRemovalDeserializer : ValueDeserializer<String?>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): String? {
        return jp.string?.trim()
    }
}