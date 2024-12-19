package no.nav.aap.komponenter.json

import java.io.IOException

public class DeserializationException(exception: IOException) : RuntimeException(exception)