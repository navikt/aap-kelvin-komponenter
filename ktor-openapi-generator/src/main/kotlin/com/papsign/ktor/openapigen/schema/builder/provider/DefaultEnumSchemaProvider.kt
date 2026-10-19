package com.papsign.ktor.openapigen.schema.builder.provider

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.util.TokenBuffer
import no.nav.aap.komponenter.json.DefaultJsonMapper
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.getKType
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.DefaultOpenAPIModule
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.schema.builder.FinalSchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.SchemaBuilder
import kotlin.reflect.KType
import kotlin.reflect.jvm.jvmErasure

object DefaultEnumSchemaProvider : SchemaBuilderProviderModule, OpenAPIGenModuleExtension, DefaultOpenAPIModule {

    private object Builder: SchemaBuilder {
        override val superType: KType = getKType<Enum<*>?>()

        override fun build(
            type: KType,
            builder: FinalSchemaBuilder,
            finalize: (SchemaModel<*>) -> SchemaModel<*>
        ): SchemaModel<*> {
            checkType(type)
            return finalize(
                SchemaModel.SchemaModelEnum<Any?>(
                    type.jvmErasure.java.enumConstants.map { serializedValue(it) }.sorted(),
                    type.isMarkedNullable
                )
            )
        }

        private fun serializedValue(value: Any): String {
            val mapper = DefaultJsonMapper.objectMapper()
            val provider = mapper.serializerProviderInstance
            // Serialize the enum value, not its enclosing polymorphic type wrapper.
            val node = TokenBuffer(mapper, false).use { buffer ->
                provider.findValueSerializer(value.javaClass).serialize(value, buffer, provider)
                buffer.asParser().use { mapper.readTree<JsonNode>(it) }
            }
            require(node.isTextual) {
                "Only string enum values are supported: ${value.javaClass.name}.$value serialized as $node"
            }
            return node.textValue()
        }
    }

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): List<SchemaBuilder> {
        return listOf(Builder)
    }
}
