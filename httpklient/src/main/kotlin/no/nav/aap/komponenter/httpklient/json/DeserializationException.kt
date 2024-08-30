package no.nav.aap.komponenter.httpklient.json

import java.io.IOException

class DeserializationException(exception: IOException) : RuntimeException(exception)