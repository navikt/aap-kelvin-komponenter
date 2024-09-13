package no.nav.aap.komponenter.httpklient.json

import java.io.IOException

public class SerializationException(exception: IOException) : RuntimeException(exception)