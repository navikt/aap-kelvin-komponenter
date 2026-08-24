package no.nav.aap.komponenter.json

import tools.jackson.core.JacksonException

public class DeserializationException(exception: JacksonException) : RuntimeException(exception)