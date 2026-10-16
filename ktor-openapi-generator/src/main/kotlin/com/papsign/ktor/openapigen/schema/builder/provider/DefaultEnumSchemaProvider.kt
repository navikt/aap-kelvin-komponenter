package com.papsign.ktor.openapigen.schema.builder.provider

import com.fasterxml.jackson.databind.JsonNode
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
            // Verdiene serialiseres av Jackson slik at @JsonProperty/@JsonValue på konstantene respekteres
            val mapper = DefaultJsonMapper.objectMapper()
            return finalize(
                SchemaModel.SchemaModelEnum<Any?>(
                    type.jvmErasure.java.enumConstants.map { mapper.valueToTree<JsonNode>(it).asText() }.sorted(),
                    type.isMarkedNullable
                )
            )
        }
    }

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): List<SchemaBuilder> {
        return listOf(Builder)
    }
}
