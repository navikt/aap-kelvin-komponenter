package no.nav.aap.komponenter.json

import java.io.IOException

public class SerializationException(exception: IOException) : RuntimeException(exception)