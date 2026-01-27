package com.papsign.ktor.openapigen.schema.builder.provider

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.KTypeProperty
import com.papsign.ktor.openapigen.OpenAPIGen
import com.papsign.ktor.openapigen.OpenAPIGenModuleExtension
import com.papsign.ktor.openapigen.classLogger
import com.papsign.ktor.openapigen.getKType
import com.papsign.ktor.openapigen.memberProperties
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.DefaultOpenAPIModule
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.ofType
import com.papsign.ktor.openapigen.schema.builder.FinalSchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.SchemaBuilder
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import io.ktor.util.reflect.platformType
import kotlin.collections.set
import kotlin.reflect.KType
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.jvmErasure

object DefaultEnumObjectSchemaProvider : SchemaBuilderProviderModule, OpenAPIGenModuleExtension, DefaultOpenAPIModule {
    private val log = classLogger()

    override fun provide(apiGen: OpenAPIGen, provider: ModuleProvider<*>): List<SchemaBuilder> {
        val namer = provider.ofType<SchemaNamer>().let { schemaNamers ->
            val last = schemaNamers.lastOrNull() ?: DefaultSchemaNamer.also {
                log.debug(
                    "No {} provided, using {}",
                    SchemaNamer::class,
                    it::class
                )
            }
            if (schemaNamers.size > 1) log.warn("Multiple ${SchemaNamer::class} provided, choosing last: ${last::class}")
            last
        }
        return listOf(Builder(apiGen, namer))
    }

    private class Builder(private val apiGen: OpenAPIGen, private val namer: SchemaNamer) : SchemaBuilder {

        override val superType: KType = getKType<ComplexEnum?>()

        private val refs = HashMap<KType, SchemaModel.SchemaModelRef<*>>()

        override fun build(type: KType,
                           builder: FinalSchemaBuilder,
                           finalize: (SchemaModel<*>) -> SchemaModel<*>): SchemaModel<*> {
            checkType(type)
            val nonNullType = type.withNullability(false)
            return refs[nonNullType] ?: {
                val erasure = nonNullType.jvmErasure
                val name = namer[nonNullType]
                val ref = SchemaModel.SchemaModelRef<Any?>("#/components/schemas/$name")
                refs[nonNullType] = ref // needed to prevent infinite recursion
                val new = if (erasure.isSealed) {
                    SchemaModel.OneSchemaModelOf(erasure.sealedSubclasses.map { builder.build(it.starProjectedType) })
                } else {
                    val props = type.memberProperties.filter { filterProperties(it) }
                        .filter { it.source.visibility == KVisibility.PUBLIC }
                    SchemaModel.SchemaModelObj(
                        props.associate {
                            Pair(extractName(it), builder.build(it.type, it.source.annotations))
                        },
                        props.filter {
                            !it.type.isMarkedNullable
                        }.map { it.name },
                    )
                }
                val final = finalize(new)
                val existing = apiGen.api.components.schemas[name]
                if (existing != null && existing != final) log.error("Schema with name $name already exists, and is not the same as the new one, replacing...")
                apiGen.api.components.schemas[name] = final
                ref
            }()
        }

        private fun extractName(it: KTypeProperty): String {
            val annotation = it.type.findAnnotation<JsonProperty>()
            val value = annotation?.value
            val useAnnotationValue = value != annotation?.defaultValue
            if (useAnnotationValue) {
                return value!!
            }
            return it.name
        }

        override fun checkType(type: KType) {
            if (type.isSubtypeOf(getKType<Enum<*>?>())) {
                val jsonFormat = (type.platformType as Class<*>).getAnnotation(JsonFormat::class.java)
                if (jsonFormat != null && jsonFormat.shape == JsonFormat.Shape.OBJECT) {
                    return
                }
            }
            error("${this::class} cannot build type $type, only subtypes of $superType are supported")
        }

        private fun filterProperties(kTypeProperty: KTypeProperty): Boolean {
            if (kTypeProperty.type.hasAnnotation<JsonIgnore>()) {
                return false
            }
            return kTypeProperty.name !in setOf("ordinal")
        }
    }
}
