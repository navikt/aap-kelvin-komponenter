package no.nav.aap.komponenter.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer


public class WhiteSpaceRemovalDeserializer : JsonDeserializer<String?>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): String? {
        return jp.text?.trim()
    }
}