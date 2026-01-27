package com.papsign.ktor.openapigen.schema.builder.provider

import com.fasterxml.jackson.annotation.JsonFormat
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
            val jsonFormat = type.javaClass.getAnnotation(JsonFormat::class.java)
            if (jsonFormat != null && jsonFormat.shape == JsonFormat.Shape.OBJECT) {
                return finalize(
                    SchemaModel.SchemaModelEnum<Any?>(
                        type.jvmErasure.java.enumConstants.map { (it as Enum<*>).name },
                        type.isMarkedNullable
                    )
                )
            }
            return finalize(
                SchemaModel.SchemaModelEnum<Any?>(
                    type.jvmErasure.java.enumConstants.map { (it as Enum<*>).name },
                    type.isMarkedNullable
                )
            )
        }
    }

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): List<SchemaBuilder> {
        return listOf(Builder)
    }
}
