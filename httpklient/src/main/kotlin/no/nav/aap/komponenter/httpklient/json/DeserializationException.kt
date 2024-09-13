package no.nav.aap.komponenter.httpklient.json

import java.io.IOException

public class DeserializationException(exception: IOException) : RuntimeException(exception)