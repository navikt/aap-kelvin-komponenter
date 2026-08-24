package no.nav.aap.komponenter.json

import tools.jackson.core.JacksonException
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.databind.cfg.EnumFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.jsontype.NamedType
import tools.jackson.module.kotlin.jacksonMapperBuilder
import tools.jackson.module.kotlin.readValue
import java.io.InputStream
import java.util.*


public object DefaultJsonMapper {

    // Subtypes registered by consumers via [registerSubtypes] before the mapper is first used.
    // Jackson 3's JsonMapper is immutable once built, so polymorphic subtypes can no longer be
    // added post-construction (unlike Jackson 2's mutable ObjectMapper) - they must go into the
    // builder chain below instead.
    private val additionalSubtypes = mutableListOf<NamedType>()

    @Volatile
    private var mapperBuilt = false

    // java.time (de)serialization support is built into jackson-databind in Jackson 3 -
    // no explicit JavaTimeModule registration needed.
    private val mapper: JsonMapper by lazy {
        mapperBuilt = true
        jacksonMapperBuilder()
            .defaultTimeZone(TimeZone.getTimeZone("Europe/Oslo"))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DateTimeFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .registerSubtypes(*additionalSubtypes.toTypedArray())
            .build()
    }

    /**
     * Registers polymorphic subtypes on the shared mapper. Must be called before any other
     * DefaultJsonMapper function (toJson/fromJson/objectMapper) - the mapper is built lazily on
     * first use and is immutable afterwards, so calling this too late throws rather than
     * silently dropping the subtypes.
     */
    @Synchronized
    public fun registerSubtypes(vararg subtypes: NamedType) {
        check(!mapperBuilt) {
            "DefaultJsonMapper has already been built - registerSubtypes() must be called before " +
                "the first use of toJson/fromJson/objectMapper()."
        }
        additionalSubtypes += subtypes
    }

    @Synchronized
    public fun registerSubtypes(vararg classes: Class<*>) {
        check(!mapperBuilt) {
            "DefaultJsonMapper has already been built - registerSubtypes() must be called before " +
                "the first use of toJson/fromJson/objectMapper()."
        }
        additionalSubtypes += classes.map { NamedType(it) }
    }

    public fun toJson(value: Any): String {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value)
        } catch (e: JacksonException) {
            throw SerializationException(e)
        }
    }

    public fun <T> fromJson(value: String, toClass: Class<T>): T {
        try {
            return mapper.readValue(value, toClass)
        } catch (e: JacksonException) {
            throw DeserializationException(e)
        }
    }

   public inline fun <reified T> fromJson(value: String): T {
        try {
            return objectMapper().readValue<T>(value)
        } catch (e: JacksonException) {
            throw DeserializationException(e)
        }
    }
   public inline fun <reified T> fromJson(value: InputStream): T {
        try {
            return objectMapper().readValue<T>(value)
        } catch (e: JacksonException) {
            throw DeserializationException(e)
        }
    }

    public fun objectMapper(): JsonMapper {
        return mapper
    }
}

