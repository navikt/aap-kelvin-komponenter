package no.nav.aap.komponenter.httpklient.json

import java.io.IOException

class SerializationException(exception: IOException) : RuntimeException(exception)