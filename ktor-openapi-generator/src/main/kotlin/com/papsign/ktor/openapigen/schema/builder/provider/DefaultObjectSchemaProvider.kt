package com.papsign.ktor.openapigen.schema.builder.provider

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import no.nav.aap.komponenter.json.DefaultJsonMapper
import com.papsign.ktor.openapigen.model.schema.Discriminator
import com.papsign.ktor.openapigen.*
import com.papsign.ktor.openapigen.model.schema.SchemaModel
import com.papsign.ktor.openapigen.modules.DefaultOpenAPIModule
import com.papsign.ktor.openapigen.modules.ModuleProvider
import com.papsign.ktor.openapigen.modules.ofType
import com.papsign.ktor.openapigen.schema.builder.FinalSchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.SchemaBuilder
import com.papsign.ktor.openapigen.schema.builder.provider.DefaultObjectSchemaProvider.branches
import com.papsign.ktor.openapigen.schema.namer.DefaultSchemaNamer
import com.papsign.ktor.openapigen.schema.namer.SchemaNamer
import kotlin.reflect.*
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.jvmErasure

object DefaultObjectSchemaProvider : SchemaBuilderProviderModule, OpenAPIGenModuleExtension,
    DefaultOpenAPIModule {
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

    private class Builder(private val apiGen: OpenAPIGen, private val namer: SchemaNamer) :
        SchemaBuilder {

        override val superType: KType = getKType<Any?>()

        private val refs = HashMap<KType, SchemaModel.SchemaModelRef<*>>()

        override fun build(
            type: KType,
            builder: FinalSchemaBuilder,
            finalize: (SchemaModel<*>) -> SchemaModel<*>
        ): SchemaModel<*> {
            checkType(type)
            val nonNullType = type.withNullability(false)
            val exists = refs[nonNullType]
            if (exists != null) {
                return exists
            }

            val erasure = nonNullType.jvmErasure
            val name = namer[nonNullType]
            val ref = SchemaModel.SchemaModelRef<Any?>("#/components/schemas/$name")
            refs[nonNullType] = ref // needed to prevent infinite recursion

            val new = if (erasure.isSealed) {
                buildSealed(nonNullType, builder)
            } else {
                buildObject(type, builder)
            }
            val final = finalize(new)
            val existing = apiGen.api.components.schemas[name]
            if (existing != null && existing != final) log.error("Schema with name $name already exists, and is not the same as the new one, replacing...")
            apiGen.api.components.schemas[name] = final
            return ref
        }

        private fun buildSealed(type: KType, builder: FinalSchemaBuilder): SchemaModel<*> {
            val erasure = type.jvmErasure
            return SchemaModel.OneSchemaModelOf<Any?>(
                erasure.sealedSubclasses.map { builder.build(branches(type, it)) },
                discriminator = typeDiscriminator(erasure)?.let { typeDiscriminator ->
                    Discriminator<Any?>(
                        typeDiscriminator.property,
                        leafTypes(type).associate { leaf ->
                            typeDiscriminator.typeId(leaf.jvmErasure) to "#/components/schemas/${namer[leaf]}"
                        }
                    )
                }
            )
        }

        private fun buildObject(type: KType, builder: FinalSchemaBuilder): SchemaModel<*> {
            val erasure = type.jvmErasure
            val props =
                type.memberProperties.filter { it.source.visibility == KVisibility.PUBLIC }
                    .filterNot { it.type.hasAnnotation<JsonIgnore>() }

            val mapper = DefaultJsonMapper.objectMapper()
            val jsonValueMember = mapper.serializationConfig
                .introspect(mapper.constructType(erasure.java))
                .findJsonValueAccessor()?.member
            val jsonValueProperty = props.firstOrNull {
                it.source.javaField == jsonValueMember || it.source.javaGetter == jsonValueMember
            }
            if (jsonValueProperty != null) {
                return builder.build(jsonValueProperty.type, jsonValueProperty.source.annotations)
            }

            val typeDiscriminator = typeDiscriminator(erasure)
            return SchemaModel.SchemaModelObj<Any?>(
                props.associate {
                    if (typeDiscriminator != null && it.name == typeDiscriminator.property) {
                        Pair(
                            it.name,
                            SchemaModel.SchemaModelEnum<String>(listOf(typeDiscriminator.typeId(erasure)))
                        )
                    } else {
                        Pair(it.name, builder.build(it.type, it.source.annotations))
                    }
                },
                props.filter { !it.type.isMarkedNullable }.map { it.name }
            )
        }
    }

    /** Jackson sin egen tolkning av @JsonTypeInfo/@JsonTypeName for typen, slik at skjemaet matcher serialiseringen. */
    private class TypeDiscriminator(val property: String, private val typeSerializer: TypeSerializer) {
        fun typeId(klass: KClass<*>): String = typeSerializer.typeIdResolver.idFromValueAndType(null, klass.java)
    }

    private fun typeDiscriminator(klass: KClass<*>): TypeDiscriminator? {
        val mapper = DefaultJsonMapper.objectMapper()
        val typeSerializer = mapper.serializerFactory
            .createTypeSerializer(mapper.serializationConfig, mapper.constructType(klass.java))
        val property = typeSerializer?.propertyName
        val storedAsProperty = typeSerializer?.typeInclusion == JsonTypeInfo.As.PROPERTY ||
                typeSerializer?.typeInclusion == JsonTypeInfo.As.EXISTING_PROPERTY
        return if (property != null && storedAsProperty &&
            typeSerializer.typeIdResolver.mechanism == JsonTypeInfo.Id.NAME
        ) TypeDiscriminator(property, typeSerializer) else null
    }

    /** Flater ut nøstede sealed-typer til konkrete subtyper. */
    private fun leafTypes(parentType: KType): List<KType> =
        parentType.jvmErasure.sealedSubclasses.flatMap {
            val sub = branches(parentType, it)
            if (it.isSealed) leafTypes(sub) else listOf(sub)
        }

    /**
     * Gitt følgende kode
     * ```
     * sealed interface Optional<A>
     * data class Some<X>(val x: X): Optional<X>
     * data class None<X>: Optional<X>
     * ```
     * og `Optional<String>`, så blir det to kall til [branches] :
     * 1. `branches( Optional<String>, Some )`
     * 2. `branches( Optional<String>, None )`
     *
     * de vil returnere henholdsvis:
     *
     * 1. `Some<String>`
     * 2. `None<String>`
     *
     *  Unifieren som bygges opp underveis er i begge tilfeller:
     *  ```
     *  mapOf( X to String )
     *  ```
     */
    fun branches(parentType: KType, subclass: KClass<*>): KType {
        val actualParametersParent = parentType.arguments
        val formalParametersParent = subclass.supertypes
            .single { it.jvmErasure == parentType.jvmErasure }
            .arguments

        val unifier =
            actualParametersParent.zip(formalParametersParent) { actual, formal ->
                unify(
                    actual = actual.type ?: return@zip mapOf(),
                    formal = formal.type ?: return@zip mapOf(),
                )
            }.fold(mapOf(), ::mergeUnifiers)

        return try {
            subclass.createType(subclass.typeParameters.map {
                KTypeProjection(
                    KVariance.INVARIANT,
                    requireNotNull(unifier[it]) { " unifier missing for $it (unifier $unifier)" })
            })
        } catch (_: Exception) {
            log.warn("unable to specialize $subclass of $parentType")
            /* fallback */
            subclass.starProjectedType
        }
    }

    fun unify(actual: KType, formal: KType): Map<KClassifier, KType> {
        return when (val classifier = formal.classifier!!) {
            is KTypeParameter ->  mapOf(classifier to actual)
            else -> error("only support for simple generic types ")
        }
    }

    fun mergeUnifiers(u1: Map<KClassifier, KType>, u2: Map<KClassifier, KType>): Map<KClassifier, KType> {
        return buildMap {
            this.putAll(u1)

            for ((k, v) in u2) {
                val existing = u1[k]
                if (existing != null) {
                    check(existing == v) { "not unifiable" }
                }
                put(k, v)
            }
        }
    }
}
