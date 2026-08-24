package no.nav.aap.komponenter.json

import tools.jackson.core.JacksonException

public class SerializationException(exception: JacksonException) : RuntimeException(exception)