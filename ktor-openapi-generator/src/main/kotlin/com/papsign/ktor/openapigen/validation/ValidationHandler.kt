package com.papsign.ktor.openapigen.validation

import com.papsign.ktor.openapigen.*
import com.papsign.ktor.openapigen.classLogger
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*


/**
 * don't mind the evil leak, it's that or a two-step builder structure to be able to handle recursive types
 */
class ValidationHandler private constructor(
    annotatedType: AnnotatedKType,
    leakThis: (ValidationHandler) -> Unit
) {

    private val log = classLogger()

    private val transformFun: ((Any?) -> Any?)?

    private fun ValidatorAnnotation.getHandlerInstance(): ValidatorBuilder<*> {
        return handler.objectInstance ?: error("${ValidatorAnnotation::class.simpleName} handler must be an object")
    }

    init {
        leakThis(this)
        val annotations = annotatedType.annotations
        val type = annotatedType.type
        val validators = annotations.mapNotNull { annot ->
            annot.annotationClass.findAnnotation<ValidatorAnnotation>()
                // Safe: ValidatorAnnotation guarantees the handler's declared annotation type matches `annot`'s class.
                ?.let { @Suppress("UNCHECKED_CAST") (it.getHandlerInstance() as ValidatorBuilder<Annotation>).build(type, annot) }
        }
        val shouldTransform = validators.isNotEmpty()
        val transform: (Any?) -> Any? = { source: Any? ->
            validators.fold(source) { value, validator -> validator.validate(value) }
        }
        when {
            type.isSubtypeOf(arrayType) -> {
                val contentType = type.arguments[0].type!!
                val handler =
                    build(
                        contentType
                    )
                when {
                    handler.isUseful() && shouldTransform -> {
                        transformFun = { t: Any? ->
                            if (t != null) {
                                val size = java.lang.reflect.Array.getLength(t)
                                for (i in 0 until size) {
                                    val value = java.lang.reflect.Array.get(t, i)
                                    java.lang.reflect.Array.set(t, i, handler.handle(value))
                                }
                            }
                            transform(t)
                        }
                    }

                    handler.isUseful() -> {
                        transformFun = { t: Any? ->
                            if (t != null) {
                                val size = java.lang.reflect.Array.getLength(t)
                                for (i in 0 until size) {
                                    val value = java.lang.reflect.Array.get(t, i)
                                    java.lang.reflect.Array.set(t, i, handler.handle(value))
                                }
                            }
                            t
                        }
                    }

                    shouldTransform -> {
                        transformFun = transform
                    }

                    else -> {
                        transformFun = null
                    }
                }
            }

            type.isSubtypeOf(iterableType) -> {
                val contentType = type.arguments[0].type!!
                val handler =
                    build(
                        contentType
                    )
                if (type.jvmErasure.isInterface) {
                    when {
                        handler.isUseful() && shouldTransform -> when {
                            type.isSubtypeOf(setType) -> {
                                transformFun = { t: Any? ->
                                    transform(
                                        if (t != null) {
                                            (t as Iterable<Any?>).map { handler.handle(it) }.toSet()
                                        } else {
                                            t
                                        }
                                    )
                                }
                            }

                            type.isSubtypeOf(listType) -> {
                                transformFun = { t: Any? ->
                                    transform(if (t != null) {
                                        (t as Iterable<Any?>).map { handler.handle(it) }
                                    } else {
                                        t
                                    })
                                }
                            }

                            else -> error("Iterable interface $type is not supported, please use List or Set")
                        }

                        handler.isUseful() -> when {
                            type.isSubtypeOf(setType) -> {
                                transformFun = { t: Any? ->
                                    if (t != null) {
                                        (t as Iterable<Any?>).map { handler.handle(it) }.toSet()
                                    } else {
                                        t
                                    }

                                }
                            }

                            type.isSubtypeOf(listType) -> {
                                transformFun = { t: Any? ->
                                    if (t != null) {
                                        (t as Iterable<Any?>).map { handler.handle(it) }
                                    } else {
                                        t
                                    }
                                }
                            }

                            else -> error("Iterable interface $type is not supported, please use List or Set")
                        }

                        shouldTransform -> {
                            transformFun = transform
                        }

                        else -> {
                            transformFun = null
                        }
                    }
                } else {
                    val appropriateConstructor = type.jvmErasure.constructors.find {
                        it.parameters.size == 1 && it.parameters[0].type.isSubtypeOf(iterableType)
                    } ?: error("Unsupported Iterable type $type, must have a constructor that takes an iterable")
                    when {
                        handler.isUseful() && shouldTransform -> {
                            transformFun = { t: Any? ->
                                if (t != null) {
                                    appropriateConstructor.call((t as Iterable<Any?>).map { handler.handle(it) })
                                } else {
                                    t
                                }.let(transform)
                            }
                        }

                        handler.isUseful() -> {
                            transformFun = { t: Any? ->
                                if (t != null) {
                                    appropriateConstructor.call((t as Iterable<Any?>).map { handler.handle(it) })
                                } else {
                                    t
                                }
                            }
                        }

                        shouldTransform -> {
                            transformFun = transform
                        }

                        else -> {
                            transformFun = null
                        }
                    }
                }
            }

            type.jvmErasure.isSealed -> {
                val possibleClasses = type.jvmErasure.sealedSubclasses
                val handlers = possibleClasses.associateWith {
                    build(
                        it.starProjectedType,
                        annotatedType.typeAnnotation + annotatedType.additionalAnnotations
                    )
                }
                val useful = handlers.values.any { it.isUseful() }
                when {
                    useful && shouldTransform -> {
                        transformFun = { t: Any? ->
                            transform(
                                if (t != null) {
                                    (handlers[t::class] ?: error("No handler for sealed class ${t::class.starProjectedType}, supposed child of $type")).handle(
                                        t
                                    )
                                } else {
                                    t
                                }
                            )
                        }
                    }

                    useful -> {
                        transformFun = { t: Any? ->
                            if (t != null) {
                                (handlers[t::class] ?: error("No handler for sealed class ${t::class.starProjectedType}, supposed child of $type")).handle(
                                    t
                                )
                            } else {
                                t
                            }
                        }
                    }

                    shouldTransform -> {
                        transformFun = transform
                    }

                    else -> {
                        transformFun = null
                    }
                }
            }

            else -> {
                if (isSubtypeOfAFunction(type)) {
                    transformFun = null
                } else {
                    val handled = type.memberProperties.mapNotNull { prop ->
                        val validator = build(prop)
                        if (validator.isUseful()) {
                            prop.source.javaField.also {
                                if (it == null) {
                                    log.warn("Field ${prop.source} could not be processed because delegated properties are not supported")
                                }
                            }?.let {
                                Triple(validator, it, prop.source)
                            }
                        } else {
                            null
                        }
                    }

                    // Reads via the Kotlin property getter (not raw java.lang.reflect.Field/Method access) so
                    // that value classes (e.g. UInt/ULong/UShort/UByte, or custom inline classes) get properly
                    // boxed instead of leaking their unboxed underlying representation. The property is forced
                    // accessible first so private properties keep working, matching the previous Field-based
                    // behaviour.
                    fun readValue(sourceProp: KProperty1<*, *>, t: Any): Any? {
                        val accessible = sourceProp.isAccessible
                        sourceProp.isAccessible = true
                        return try {
                            sourceProp.getter.call(t)
                        } finally {
                            sourceProp.isAccessible = accessible
                        }
                    }

                    // Kotlin unsigned value classes (UByte/UShort/UInt/ULong) are backed on the JVM by their
                    // unboxed underlying primitive. java.lang.reflect.Field.set cannot unbox a boxed instance
                    // of one of these types into that primitive field, so any direct field write must convert
                    // it first.
                    fun unboxUnsigned(value: Any?): Any? = when (value) {
                        is UByte -> value.toByte()
                        is UShort -> value.toShort()
                        is UInt -> value.toInt()
                        is ULong -> value.toLong()
                        else -> value
                    }

                    when {
                        handled.isNotEmpty() && shouldTransform -> {
                            transformFun = { t: Any? ->
                                if (t != null) {
                                    handled.forEach { (handler, field, sourceProp) ->
                                        val accessible = field.canAccess(t)
                                        field.setAccessible(true)
                                        field.set(t, unboxUnsigned(handler.handle(readValue(sourceProp, t))))
                                        field.setAccessible(accessible)
                                    }
                                }
                                transform(t)
                            }
                        }

                        handled.isNotEmpty() -> {
                            transformFun = { t: Any? ->
                                if (t != null) {
                                    val copy = t.javaClass.kotlin.memberFunctions.find { it.name == "copy" }
                                    val copyParams =
                                        copy?.instanceParameter?.let { mutableMapOf<KParameter, Any?>(it to t) }
                                    handled.forEach { (handler, field, sourceProp) ->
                                        val newValue = handler.handle(readValue(sourceProp, t))
                                        if (copy != null && copyParams != null) {
                                            val param = copy.parameters.first { it.name == field.name }
                                            copyParams[param] = newValue
                                        } else {
                                            @Suppress("UNCHECKED_CAST")
                                            val mutableProp = sourceProp as? KMutableProperty1<Any, Any?>
                                            if (mutableProp != null) {
                                                // Use the Kotlin property setter (not java.lang.reflect.Field.set)
                                                // so value classes are correctly unboxed to their underlying JVM
                                                // representation before being written to the backing field.
                                                val accessible = mutableProp.isAccessible
                                                mutableProp.isAccessible = true
                                                try {
                                                    mutableProp.setter.call(t, newValue)
                                                } finally {
                                                    mutableProp.isAccessible = accessible
                                                }
                                            } else {
                                                // Read-only (val) property with no copy(): the backing field's
                                                // JVM type is the unboxed primitive for unsigned value classes
                                                // (e.g. `int` for UInt), so Field.set must be given that
                                                // primitive rather than the boxed UInt/ULong/UShort/UByte
                                                // instance, which it cannot unbox on its own.
                                                // TODO convert this to canAccess and only change status if false
                                                val accessible = field.canAccess(t)
                                                field.setAccessible(true)
                                                field.set(t, unboxUnsigned(newValue))
                                                field.setAccessible(accessible)
                                            }
                                        }
                                    }
                                    if (copy != null && copyParams != null) {
                                        copy.callBy(copyParams)
                                    } else t
                                } else t
                            }
                        }

                        shouldTransform -> {
                            transformFun = transform
                        }

                        else -> {
                            transformFun = null
                        }
                    }
                }
            }
        }
    }

    private fun isSubtypeOfAFunction(type: KType): Boolean {
        return type.isSubtypeOf(getKType<Function<*>>())
    }

    fun <T> handle(t: T): T {
        // Safe: transformFun is built from the same type T that this handler was constructed for.
        @Suppress("UNCHECKED_CAST")
        return if (t != null) transformFun?.invoke(t) as T ?: t else t
    }

    fun isUseful(): Boolean {
        return transformFun != null
    }

    companion object {

        /**
         * needed because a type is equal to another no matter the annotations
         * @property annotations, be careful that it contains everything, the code may fully rely on it
         */
        data class AnnotatedKType(
            val type: KType,
            val additionalAnnotations: List<Annotation> = listOf(),
            val typeAnnotation: List<Annotation> = type.annotations,
            val classAnnotation: List<Annotation> = type.jvmErasure.annotations
        ) {
            val annotations: List<Annotation>
                get() = classAnnotation + typeAnnotation + additionalAnnotations

            companion object {
                operator fun <T : Any> invoke(tClass: KClass<T>,
                                              annotations: List<Annotation> = listOf()): AnnotatedKType {
                    val type = tClass.starProjectedType
                    return AnnotatedKType(
                        type,
                        annotations
                    )
                }

                operator fun invoke(prop: KTypeProperty): AnnotatedKType {
                    return AnnotatedKType(
                        prop.type,
                        prop.source.annotations
                    )
                }
            }
        }

        private val map = HashMap<String, ValidationHandler>()

        /**
         * Black Magic: DO NOT TOUCH
         * We use a string because it accounts for the annotations
         */
        fun build(type: AnnotatedKType): ValidationHandler {
            val str = type.toString()
            return map[str] ?: ValidationHandler(type) {
                map[str] = it
            }
        }

        fun <T : Any> build(tClass: KClass<T>, annotations: List<Annotation> = listOf()): ValidationHandler {
            return build(
                AnnotatedKType(
                    tClass.starProjectedType,
                    annotations
                )
            )
        }

        fun build(prop: KTypeProperty): ValidationHandler {
            return build(
                AnnotatedKType(
                    prop
                )
            )
        }

        fun build(type: KType, annotations: List<Annotation> = listOf()): ValidationHandler {
            return build(
                AnnotatedKType(
                    type,
                    annotations
                )
            )
        }

        private val arrayType = getKType<Array<*>?>()
        private val iterableType = getKType<Iterable<*>?>()
        private val listType = getKType<List<*>?>()
        private val setType = getKType<Set<*>?>()
    }
}

