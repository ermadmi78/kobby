package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_CREATOR
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_INCLUDE
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_SUB_TYPES
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_SUB_TYPES_TYPE
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_TYPE_INFO
import io.github.ermadmi78.kobby.generator.kotlin.JacksonAnnotations.JSON_TYPE_NAME
import io.github.ermadmi78.kobby.model.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.ENUM
import java.io.File
import java.io.IOException
import java.io.Serializable
import java.nio.file.Path

/**
 * Created on 18.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class KotlinLayout(
    val scalars: Map<String, KotlinType>,
    val context: KotlinContextLayout,
    val dto: KotlinDtoLayout,
    val entity: KotlinEntityLayout,
    val impl: KotlinImplLayout,
    val adapter: KotlinAdapterLayout
) {
    private fun getScalarType(name: String): KotlinType = scalars[name]
        ?: throw IllegalStateException(
            "Kotlin data type for scalar '$name' not found. " +
                    "Please, configure it by means of 'kobby' extension. https://github.com/ermadmi78/kobby"
        )

    // *****************************************************************************************************************
    //                                          DTO
    // *****************************************************************************************************************

    internal val KobbyType.dtoType: TypeName
        get() = when (this) {
            is KobbyListType -> LIST.parameterizedBy(nested.dtoType)
            is KobbyNodeType -> when (node.kind) {
                SCALAR -> getScalarType(node.name).typeName
                else -> node.dtoClass
            }
        }.let { if (nullable) it.nullable() else it }

    internal val KobbyType.dtoTypeWithSerializer: TypeName
        get() = when (this) {
            is KobbyListType -> LIST.parameterizedBy(nested.dtoTypeWithSerializer)
            is KobbyNodeType -> when (node.kind) {
                SCALAR -> getScalarType(node.name).typeNameWithSerializer
                else -> node.dtoClass
            }
        }.let { if (nullable) it.nullable() else it }

    internal val KobbyNode.dtoName: String
        get() = when (kind) {
            ENUM -> name.decorate(dto.enumDecoration)
            INPUT -> name.decorate(dto.inputDecoration)
            else -> name.decorate(dto.decoration)
        }

    internal val KobbyNode.dtoClass: ClassName
        get() = ClassName(dto.packageName, dtoName)

    internal val KobbyNode.dtoLambda: LambdaTypeName
        get() = LambdaTypeName.get(dtoClass, emptyList(), UNIT)

    internal val KobbyNode.builderName: String
        get() = dtoName.decorate(dto.builder.decoration)

    internal val KobbyNode.builderClass: ClassName
        get() = ClassName(dto.packageName, builderName)

    internal val KobbyNode.builderLambda: LambdaTypeName
        get() = LambdaTypeName.get(builderClass, emptyList(), UNIT)

    internal fun KobbyLiteral.buildInitializer(type: KobbyType, args: MutableList<Any?>): String = when (this) {
        is KobbyNullLiteral -> "null"
        is KobbyBooleanLiteral -> {
            args.add(value)
            "%L"
        }

        is KobbyIntLiteral -> {
            val kotlinType: KotlinType? = type.node.takeIf { it.kind == SCALAR }?.let { scalars[it.name] }
            if (KotlinTypes.LONG == kotlinType) {
                args.add(value.toLong())
                "%LL"
            } else {
                args.add(value.toInt())
                "%L"
            }
        }

        is KobbyFloatLiteral -> {
            val kotlinType: KotlinType? = type.node.takeIf { it.kind == SCALAR }?.let { scalars[it.name] }
            if (KotlinTypes.DOUBLE == kotlinType) {
                args.add(value.toDouble())
                "%L"
            } else {
                args.add(value.toFloat())
                "%LF"
            }
        }

        is KobbyStringLiteral -> {
            args.add(value)
            "%S"
        }

        is KobbyEnumLiteral -> {
            if (type.node.kind == ENUM) {
                args.add(type.node.dtoClass)
                "%T.$name"
            } else {
                name
            }
        }

        is KobbyListLiteral -> {
            val nestedType = type.nestedOrNull
            if (nestedType != null) {
                args.add(
                    ClassName("kotlin.collections", "listOf")
                        .parameterizedBy(nestedType.dtoType)
                )
                if (values.isEmpty()) {
                    "%T()"
                } else {
                    val sb = StringBuilder()
                    sb.append("%T(")
                    var first = true
                    for (nestedLiteral in values) {
                        if (first) {
                            first = false
                        } else {
                            sb.append(", ")
                        }
                        sb.append(nestedLiteral.buildInitializer(nestedType, args))
                    }
                    sb.append(")")
                    sb.toString()
                }
            } else {
                args.add(
                    ClassName("kotlin.collections", "listOf")
                        .parameterizedBy(ANY.nullable())
                )
                "%T()"
            }
        }

        is KobbyObjectLiteral -> {
            if (type.node.kind == INPUT) {
                args.add(type.dtoType)
                if (values.isEmpty()) {
                    "%T()"
                } else {
                    val sb = StringBuilder()
                    sb.append("%T(")
                    var first = true
                    for (nestedPair in values) {
                        val nestedField = type.node.fields[nestedPair.key] ?: continue
                        if (first) {
                            first = false
                        } else {
                            sb.append(", ")
                        }

                        sb.append(nestedPair.key).append(" = ")
                        sb.append(nestedPair.value.buildInitializer(nestedField.type, args))
                    }
                    sb.append(")")
                    sb.toString()
                }
            } else {
                type.node.name
            }
        }

        is KobbyVariableLiteral -> name
    }

    // *****************************************************************************************************************
    //                                          Entity
    // *****************************************************************************************************************
    internal val KobbyType.hasProjection: Boolean
        get() = when (node.kind) {
            OBJECT, INTERFACE, UNION -> true
            else -> false
        }

    internal val KobbyNode.hasDefaults: Boolean
        get() = when (kind) {
            OBJECT -> fields.values.any { it.isRequired || it.isDefault }
            else -> true // unions and interfaces always has default projection __typename
        }

    internal val KobbyField.entityType: TypeName
        get() = type.toEntityType(false)

    internal val KobbyArgument.entityType: TypeName
        get() = type.toEntityType(hasDefaultValue)

    internal val KobbyArgument.entityTypeWithSerializer: TypeName
        get() = type.toEntityTypeWithSerializer(hasDefaultValue)

    private fun KobbyType.toEntityType(makeNullable: Boolean): TypeName = when (this) {
        is KobbyListType -> LIST.parameterizedBy(nested.toEntityType(false))
        is KobbyNodeType -> when (node.kind) {
            SCALAR -> getScalarType(node.name).typeName
            ENUM, INPUT -> node.dtoClass
            else -> node.entityClass
        }
    }.let { if (nullable || makeNullable) it.nullable() else it }

    private fun KobbyType.toEntityTypeWithSerializer(makeNullable: Boolean): TypeName = when (this) {
        is KobbyListType -> LIST.parameterizedBy(nested.toEntityTypeWithSerializer(false))
        is KobbyNodeType -> when (node.kind) {
            SCALAR -> getScalarType(node.name).typeNameWithSerializer
            ENUM, INPUT -> node.dtoClass
            else -> node.entityClass
        }
    }.let { if (nullable || makeNullable) it.nullable() else it }

    internal val KobbyNode.entityName: String
        get() = when (kind) {
            ENUM, INPUT -> error("Invalid algorithm - try to make entity of ${kind.name}")
            else -> name.decorate(entity.decoration)
        }

    internal val KobbyNode.entityClass: ClassName
        get() = ClassName(entity.packageName, entityName)

    internal val KobbyNode.projectionName: String
        get() = when (kind) {
            OBJECT, INTERFACE, UNION -> name.decorate(entity.projection.projectionDecoration)
            else -> error("Invalid algorithm - try to make projection of ${kind.name}")
        }

    internal val KobbyNode.projectionClass: ClassName
        get() = ClassName(entity.packageName, projectionName)

    internal val KobbyNode.projectionLambda: LambdaTypeName
        get() = LambdaTypeName.get(qualifiedProjectionClass, emptyList(), UNIT)

    internal val KobbyField.projectionFieldName: String
        get() {
            var res = if (isDefault) {
                name.decorate(entity.projection.withoutDecoration)
            } else {
                name.decorate(entity.projection.withDecoration)
            }

            if (res in FORBIDDEN_PROJECTION_NAMES) {
                res = "__$res"
            }

            return res
        }

    internal val KobbyNode.qualificationName: String
        get() = when (kind) {
            INTERFACE, UNION -> name.decorate(entity.projection.qualificationDecoration)
            else -> error("Invalid algorithm - try to make qualification of ${kind.name}")
        }

    internal val KobbyNode.qualificationClass: ClassName
        get() = ClassName(entity.packageName, qualificationName)

    internal val KobbyNode.qualifiedProjectionName: String
        get() = when (kind) {
            INTERFACE, UNION -> name.decorate(entity.projection.qualifiedProjectionDecoration)
            else -> projectionName
        }

    internal val KobbyNode.qualifiedProjectionClass: ClassName
        get() = ClassName(entity.packageName, qualifiedProjectionName)

    internal val KobbyNode.projectionOnName: String
        get() = name.decorate(entity.projection.onDecoration)

    internal val KobbyField.selectionName: String
        get() = overriddenField?.selectionName
            ?: "${node.name}${name._capitalize()}".decorate(entity.selection.selectionDecoration)

    internal val KobbyField.selectionClass: ClassName
        get() = overriddenField?.selectionClass
            ?: ClassName(entity.packageName, selectionName)

    internal val KobbyField.queryName: String
        get() = overriddenField?.queryName
            ?: "${node.name}${name._capitalize()}".decorate(entity.selection.queryDecoration)

    internal val KobbyField.queryClass: ClassName
        get() = overriddenField?.queryClass
            ?: ClassName(entity.packageName, queryName)

    internal val KobbyField.lambda: Pair<String, LambdaTypeName>?
        get() = overriddenField?.lambda
            ?: if (isSelection) {
                if (type.hasProjection) {
                    entity.selection.queryArgument to
                            LambdaTypeName.get(queryClass, emptyList(), UNIT)
                } else {
                    entity.selection.selectionArgument to
                            LambdaTypeName.get(selectionClass, emptyList(), UNIT)
                }
            } else {
                if (type.hasProjection) {
                    entity.projection.projectionArgument to type.node.projectionLambda
                } else {
                    null
                }
            }

    // *****************************************************************************************************************
    //                                          Impl
    // *****************************************************************************************************************

    internal val KobbyNode.implName: String
        get() = name.decorate(impl.decoration)

    internal val KobbyNode.implClass: ClassName
        get() = ClassName(impl.packageName, implName)

    internal val KobbyNode.implProjectionName: String
        get() = qualifiedProjectionName.decorate(impl.decoration)

    internal val KobbyNode.implProjectionClass: ClassName
        get() = ClassName(impl.packageName, implProjectionName)

    internal val KobbyNode.implProjectionProperty: Pair<String, TypeName>
        get() = impl.projectionPropertyName to implProjectionClass

    internal val KobbyField.implQueryName: String
        get() = queryName.decorate(impl.decoration)

    internal val KobbyField.implQueryClass: ClassName
        get() = ClassName(impl.packageName, implQueryName)

    internal val KobbyField.implSelectionName: String
        get() = selectionName.decorate(impl.decoration)

    internal val KobbyField.implSelectionClass: ClassName
        get() = ClassName(impl.packageName, implSelectionName)

    internal val KobbyNode.innerProjectionOnName: String
        get() = "_" + projectionOnName.decorate(impl.innerDecoration)

    internal val KobbyNode.entityBuilderName: String
        get() = "buildEntity"

    internal val KobbyField.innerName: String
        get() = (name + number).decorate(impl.innerDecoration)

    internal val KobbyField.innerClass: ClassName
        get() = if (isSelection) {
            if (type.hasProjection) implQueryClass else implSelectionClass
        } else {
            if (type.hasProjection) type.node.implProjectionClass else BOOLEAN
        }

    internal val KobbyField.innerType: TypeName
        get() = if (innerIsBoolean) BOOLEAN else innerClass.nullable()

    internal val KobbyField.innerIsBoolean: Boolean
        get() = !isSelection && !type.hasProjection

    internal val KobbyField.innerInitializer: String
        get() = when {
            !innerIsBoolean -> "null"
            isDefault -> "true"
            else -> "false"
        }

    internal val KobbyField.resolverName: String
        get() = name.decorate("resolve", null)

    internal val KobbyField.notNullAssertion: String
        get() = if (type.nullable) "" else "!!"

    internal val KobbyField.noProjectionMessage: String
        get() = "Property [$name] is not available - " +
                "${if (isDefault) "remove" else "add"} [$projectionFieldName] projection to switch on it"

    internal val KobbyField.projectionBuilderFunName: String
        get() = "__" + "build${name._capitalize()}${number}".decorate(impl.innerDecoration)

    internal val KobbyArgument.innerName: String
        get() = (field.name + field.number + name._capitalize())
            .decorate(impl.innerDecoration)

    internal val KobbyField.isProjectionPropertyEnabled: Boolean
        get() = entity.projection.enableNotationWithoutParentheses && !isDefault && isProperty

    // *****************************************************************************************************************
    //                                          Context
    // *****************************************************************************************************************
    internal val KobbySchema.receiverSubscriptionDtoLambda: LambdaTypeName
        get() = LambdaTypeName.get(
            context.receiverClass.parameterizedBy(
                if (adapter.extendedApi) dto.graphql.subscriptionResultClass else subscription.dtoClass
            ),
            emptyList(),
            UNIT
        ).copy(suspending = true)

    //******************************************************************************************************************
    //                                     Kotlinx Serialization
    //******************************************************************************************************************

    internal fun TypeSpecBuilder.annotateSerializable(): TypeSpecBuilder {
        if (dto.serialization.enabled) {
            buildAnnotation(SerializationAnnotations.SERIALIZABLE)
        }
        return this
    }

    internal fun TypeSpecBuilder.annotateSerialName(name: String): TypeSpecBuilder {
        if (dto.serialization.enabled) {
            buildAnnotation(SerializationAnnotations.SERIAL_NAME) {
                addMember("%S", name)
            }
        }
        return this
    }

    internal fun TypeSpecBuilder.enableExperimentalSerializationApi(): TypeSpecBuilder {
        if (dto.serialization.enabled) {
            buildAnnotation(KotlinAnnotations.OPT_IN) {
                addMember("%T::class", SerializationAnnotations.ExperimentalSerializationApi)
            }
        }
        return this
    }

    internal fun TypeSpecBuilder.annotateJsonClassDiscriminator(discriminator: String): TypeSpecBuilder {
        if (dto.serialization.enabled) {
            buildAnnotation(SerializationAnnotations.JSON_CLASS_DISCRIMINATOR) {
                addMember("%S", discriminator)
            }
        }
        return this
    }

    //******************************************************************************************************************
    //                                          Jackson
    //******************************************************************************************************************

    internal fun TypeSpecBuilder.jacksonizeClass(node: KobbyNode): TypeSpecBuilder {
        if (!dto.jackson.enabled) {
            return this
        }

        if (node.kind != INPUT) {
            buildAnnotation(JSON_TYPE_NAME) {
                addMember("value = %S", node.name)
            }

            buildAnnotation(JSON_TYPE_INFO) {
                addMember("use = %T.Id.${dto.jackson.typeInfoUse}", JSON_TYPE_INFO)
                addMember("include = %T.As.${dto.jackson.typeInfoInclude}", JSON_TYPE_INFO)
                addMember("property = %S", dto.jackson.typeInfoProperty)
                addMember("defaultImpl = %T::class", node.dtoClass)
            }
        }

        buildAnnotation(JSON_INCLUDE) {
            addMember("value = %T.Include.${dto.jackson.jsonInclude}", JSON_INCLUDE)
        }

        return this
    }

    internal fun TypeSpecBuilder.jacksonizeInterface(node: KobbyNode): TypeSpecBuilder {
        if (!dto.jackson.enabled) {
            return this
        }

        buildAnnotation(JSON_TYPE_INFO) {
            addMember("use = %T.Id.NAME", JSON_TYPE_INFO)
            addMember("include = %T.As.PROPERTY", JSON_TYPE_INFO)
            addMember("property = %S", "__typename")
        }

        buildAnnotation(JSON_SUB_TYPES) {
            for (subObjectNode in node.subObjects) {
                buildAnnotation(JSON_SUB_TYPES_TYPE) {
                    addMember("value = %T::class", subObjectNode.dtoClass)
                    addMember("name = %S", subObjectNode.name)
                }
            }
        }

        return this
    }

    internal fun FunSpecBuilder.jacksonizeConstructor(): FunSpecBuilder {
        if (dto.jackson.enabled && parameters.size == 1) {
            addAnnotation(JSON_CREATOR)
        }

        return this
    }

    internal fun PropertySpecBuilder.jacksonIncludeNonAbsent(): PropertySpecBuilder =
        jacksonInclude(JacksonInclude.NON_ABSENT)

    internal fun PropertySpecBuilder.jacksonIncludeNonEmpty(): PropertySpecBuilder =
        jacksonInclude(JacksonInclude.NON_EMPTY)

    internal fun PropertySpecBuilder.jacksonInclude(include: JacksonInclude): PropertySpecBuilder {
        if (dto.jackson.enabled) {
            buildAnnotation(JSON_INCLUDE) {
                addMember("value = %T.Include.${include.name}", JSON_INCLUDE)
            }
        }
        return this
    }
}

class KotlinContextLayout(
    packageName: String,
    val name: String,
    val decoration: Decoration,
    val query: String,
    val mutation: String,
    val subscription: String,
    val commitEnabled: Boolean
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinDtoLayout(
    packageName: String,
    val decoration: Decoration,
    val enumDecoration: Decoration,
    val inputDecoration: Decoration,
    val applyPrimaryKeys: Boolean,
    val maxNumberOfFieldsForImmutableDtoClass: Int,
    val maxNumberOfFieldsForImmutableInputClass: Int,
    val serialization: KotlinDtoSerialization,
    val jackson: KotlinDtoJacksonLayout,
    val builder: KotlinDtoBuilderLayout,
    val graphql: KotlinDtoGraphQLLayout
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinDtoSerialization(
    val enabled: Boolean,
    val classDiscriminator: String,
    val ignoreUnknownKeys: Boolean,
    val encodeDefaults: Boolean,
    val prettyPrint: Boolean
)

data class KotlinDtoJacksonLayout(
    val enabled: Boolean,
    val typeInfoUse: String,
    val typeInfoInclude: String,
    val typeInfoProperty: String,
    val jsonInclude: String
)

data class KotlinDtoBuilderLayout(
    val enabled: Boolean,
    val decoration: Decoration,
    val toBuilderFun: String,
    val toDtoFun: String,
    val toInputFun: String,
    val copyFun: String
)

class KotlinDtoGraphQLLayout(
    val enabled: Boolean,
    packageName: String,
    val decoration: Decoration
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinEntityLayout(
    val enabled: Boolean,
    packageName: String,
    val decoration: Decoration,
    val errorsFunName: String,
    val extensionsFunName: String,
    val contextFunEnabled: Boolean,
    val contextFunName: String,
    val withCurrentProjectionFun: String,
    val projection: KotlinEntityProjectionLayout,
    val selection: KotlinEntitySelectionLayout
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinEntityProjectionLayout(
    val projectionDecoration: Decoration,
    val projectionArgument: String,
    val withDecoration: Decoration,
    val withoutDecoration: Decoration,
    val minimizeFun: String,
    val qualificationDecoration: Decoration,
    val qualifiedProjectionDecoration: Decoration,
    val onDecoration: Decoration,
    val enableNotationWithoutParentheses: Boolean
)

class KotlinEntitySelectionLayout(
    val selectionDecoration: Decoration,
    val selectionArgument: String,
    val queryDecoration: Decoration,
    val queryArgument: String
)

class KotlinImplLayout(
    packageName: String,
    val decoration: Decoration,
    val internal: Boolean,
    val innerDecoration: Decoration
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinAdapterLayout(
    val extendedApi: Boolean,
    val throwException: Boolean,
    val ktor: KotlinAdapterKtorLayout
)

class KotlinAdapterKtorLayout(
    val simpleEnabled: Boolean,
    val compositeEnabled: Boolean,
    packageName: String,
    val decoration: Decoration,
    val receiveTimeoutMillis: Long?
) {
    val packageName: String = packageName.validateKotlinPath()
}

interface KotlinFile {
    @Throws(IOException::class)
    fun writeTo(out: Appendable)

    @Throws(IOException::class)
    fun writeTo(directory: Path)

    @Throws(IOException::class)
    fun writeTo(directory: File)
}

class KotlinType(
    /** A fully-qualified package name. For example: kotlin.collections */
    packageName: String,

    /** A fully-qualified class name. For example: Map.Entry */
    className: String,

    val allowNull: Boolean = false,

    /** List of generics */
    val generics: List<KotlinType> = listOf(),

    val serializer: SerializerType? = null
) : Serializable {
    val packageName: String = packageName.validateKotlinPath()

    val className: String = className.validateKotlinPath()

    fun nullable(): KotlinType = if (allowNull) this else KotlinType(
        packageName,
        className,
        true,
        generics,
        serializer
    )

    fun parameterize(vararg arguments: KotlinType): KotlinType = if (arguments.isEmpty()) this else KotlinType(
        packageName,
        className,
        allowNull,
        generics + arguments,
        serializer
    )

    fun nested(nestedClassName: String): KotlinType = KotlinType(
        packageName,
        "$className.$nestedClassName",
        allowNull,
        generics,
        serializer
    )

    /**
     * Specify a custom serializer for this type.
     */
    fun serializer(
        /** A fully-qualified package name.*/
        packageName: String,

        /** A fully-qualified class name.*/
        className: String
    ): KotlinType = KotlinType(
        this.packageName,
        this.className,
        allowNull,
        generics,
        SerializerType(packageName, className)
    )

    override fun toString(): String {
        return "$packageName.$className" +
                (if (generics.isNotEmpty()) "<${generics.joinToString { it.toString() }}>" else "") +
                (if (allowNull) "?" else "")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinType
        return packageName == other.packageName &&
                className == other.className &&
                allowNull == other.allowNull &&
                generics == other.generics
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        result = 31 * result + allowNull.hashCode()
        result = 31 * result + generics.hashCode()
        return result
    }
}

class SerializerType(
    /** A fully-qualified package name.*/
    packageName: String,

    /** A fully-qualified class name.*/
    className: String
) : Serializable {
    val packageName: String = packageName.validateKotlinPath()

    val className: String = className.validateKotlinPath()

    override fun toString(): String = "$packageName.$className"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SerializerType
        return packageName == other.packageName && className == other.className
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + className.hashCode()
        return result
    }
}

object KotlinTypes {
    val ANY by lazy { KotlinType("kotlin", "Any") }
    val NUMBER by lazy { KotlinType("kotlin", "Number") }
    val BYTE by lazy { KotlinType("kotlin", "Byte") }
    val SHORT by lazy { KotlinType("kotlin", "Short") }
    val INT = KotlinType("kotlin", "Int")
    val LONG = KotlinType("kotlin", "Long")
    val FLOAT by lazy { KotlinType("kotlin", "Float") }
    val DOUBLE = KotlinType("kotlin", "Double")
    val CHAR by lazy { KotlinType("kotlin", "Char") }
    val STRING = KotlinType("kotlin", "String")
    val CHAR_SEQUENCE by lazy { KotlinType("kotlin", "CharSequence") }
    val BOOLEAN = KotlinType("kotlin", "Boolean")

    val ENUM by lazy { KotlinType("kotlin", "Enum") }
    val THROWABLE by lazy { KotlinType("kotlin", "Throwable") }
    val COMPARABLE by lazy { KotlinType("kotlin", "Comparable") }

    val ITERABLE by lazy { KotlinType("kotlin.collections", "Iterable") }
    val COLLECTION by lazy { KotlinType("kotlin.collections", "Collection") }
    val LIST by lazy { KotlinType("kotlin.collections", "List") }
    val SET by lazy { KotlinType("kotlin.collections", "Set") }
    val MAP by lazy { KotlinType("kotlin.collections", "Map") }

    val MUTABLE_ITERABLE by lazy { KotlinType("kotlin.collections", "MutableIterable") }
    val MUTABLE_COLLECTION by lazy { KotlinType("kotlin.collections", "MutableCollection") }
    val MUTABLE_LIST by lazy { KotlinType("kotlin.collections", "MutableList") }
    val MUTABLE_SET by lazy { KotlinType("kotlin.collections", "MutableSet") }
    val MUTABLE_MAP by lazy { KotlinType("kotlin.collections", "MutableMap") }

    val ARRAY by lazy { KotlinType("kotlin", "Array") }
    val BOOLEAN_ARRAY by lazy { KotlinType("kotlin", "BooleanArray") }
    val BYTE_ARRAY by lazy { KotlinType("kotlin", "ByteArray") }
    val CHAR_ARRAY by lazy { KotlinType("kotlin", "CharArray") }
    val SHORT_ARRAY by lazy { KotlinType("kotlin", "ShortArray") }
    val INT_ARRAY by lazy { KotlinType("kotlin", "IntArray") }
    val LONG_ARRAY by lazy { KotlinType("kotlin", "LongArray") }
    val FLOAT_ARRAY by lazy { KotlinType("kotlin", "FloatArray") }
    val DOUBLE_ARRAY by lazy { KotlinType("kotlin", "DoubleArray") }

    val PREDEFINED_SCALARS: Map<String, KotlinType> = mapOf(
        "ID" to LONG,
        "Int" to INT,
        "Long" to LONG,
        "Float" to DOUBLE,
        "Double" to DOUBLE,
        "String" to STRING,
        "Boolean" to BOOLEAN
    )
}
