package io.github.ermadmi78.kobby.generator.kotlin

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
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
    val impl: KotlinImplLayout
) {
    // *****************************************************************************************************************
    //                                          DTO
    // *****************************************************************************************************************

    internal val KobbyType.dtoType: TypeName
        get() = when (this) {
            is KobbyListType -> LIST.parameterizedBy(nested.dtoType)
            is KobbyNodeType -> when (node.kind) {
                SCALAR -> scalars[node.name]!!.typeName
                else -> node.dtoClass
            }
        }.let { if (nullable) it.nullable() else it }

    internal val KobbyNode.dtoName: String
        get() = when (kind) {
            ENUM, INPUT -> name
            else -> name.decorate(dto.decoration)
        }

    internal val KobbyNode.dtoClass: ClassName
        get() = ClassName(dto.packageName, dtoName)

    internal val KobbyNode.builderName: String
        get() = dtoName.decorate(dto.builder.decoration)

    internal val KobbyNode.builderClass: ClassName
        get() = ClassName(dto.packageName, builderName)

    internal val KobbyNode.builderLambda: LambdaTypeName
        get() = LambdaTypeName.get(builderClass, emptyList(), UNIT)

    // *****************************************************************************************************************
    //                                          Entity
    // *****************************************************************************************************************
    internal val KobbyType.hasProjection: Boolean
        get() = when (node.kind) {
            OBJECT, INTERFACE, UNION -> true
            else -> false
        }

    internal val KobbyField.entityType: TypeName
        get() = type.toEntityType(false)

    internal val KobbyArgument.entityType: TypeName
        get() = type.toEntityType(hasDefaultValue)

    private fun KobbyType.toEntityType(makeNullable: Boolean): TypeName = when (this) {
        is KobbyListType -> LIST.parameterizedBy(nested.toEntityType(false))
        is KobbyNodeType -> when (node.kind) {
            SCALAR -> scalars[node.name]!!.typeName
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
        get() = if (isDefault)
            name.decorate(entity.projection.withoutDecoration)
        else
            name.decorate(entity.projection.withDecoration)

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
            ?: "${node.name}${name.capitalize()}".decorate(entity.selection.selectionDecoration)

    internal val KobbyField.selectionClass: ClassName
        get() = overriddenField?.selectionClass
            ?: ClassName(entity.packageName, selectionName)

    internal val KobbyField.queryName: String
        get() = overriddenField?.queryName
            ?: "${node.name}${name.capitalize()}".decorate(entity.selection.queryDecoration)

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

    internal val KobbyArgument.innerName: String
        get() = (field.name + field.number + name.capitalize())
            .decorate(impl.innerDecoration)


    //******************************************************************************************************************
    //                                          Jackson
    //******************************************************************************************************************

    internal fun TypeSpecBuilder.jacksonize(node: KobbyNode): TypeSpecBuilder {
        if (!dto.jackson.enabled) {
            return this
        }

        addAnnotation(
            AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_NAME)
                .addMember("value = %S", node.name)
                .build()
        )

        addAnnotation(
            AnnotationSpec.builder(JacksonAnnotations.JSON_TYPE_INFO)
                .addMember("use = %T.Id.NAME", JacksonAnnotations.JSON_TYPE_INFO)
                .addMember("include = %T.As.PROPERTY", JacksonAnnotations.JSON_TYPE_INFO)
                .addMember("property = %S", "__typename")
                .addMember("defaultImpl = ${node.dtoName}::class")
                .build()
        )

        addAnnotation(
            AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
                .addMember("value = %T.Include.NON_ABSENT", JacksonAnnotations.JSON_INCLUDE)
                .build()
        )

        return this
    }

    internal fun FunSpecBuilder.jacksonize(): FunSpecBuilder {
        if (dto.jackson.enabled && parameters.size == 1) {
            addAnnotation(JacksonAnnotations.JSON_CREATOR)
        }

        return this
    }

    internal fun PropertySpecBuilder.jacksonIncludeNonAbsent(): PropertySpecBuilder =
        jacksonInclude(JacksonInclude.NON_ABSENT)

    internal fun PropertySpecBuilder.jacksonIncludeNonEmpty(): PropertySpecBuilder =
        jacksonInclude(JacksonInclude.NON_EMPTY)

    internal fun PropertySpecBuilder.jacksonInclude(include: JacksonInclude): PropertySpecBuilder {
        if (dto.jackson.enabled) {
            addAnnotation(
                AnnotationSpec.builder(JacksonAnnotations.JSON_INCLUDE)
                    .addMember("value = %T.Include.${include.name}", JacksonAnnotations.JSON_INCLUDE)
                    .build()
            )
        }
        return this
    }
}

class KotlinContextLayout(
    packageName: String,
    val name: String,
    val decoration: Decoration,
    val query: String,
    val mutation: String
) {
    val packageName: String = packageName.validateKotlinPath()
}

class KotlinDtoLayout(
    packageName: String,
    val decoration: Decoration,
    val jackson: KotlinDtoJacksonLayout,
    val builder: KotlinDtoBuilderLayout,
    val graphql: KotlinDtoGraphQLLayout
) {
    val packageName: String = packageName.validateKotlinPath()
}

data class KotlinDtoJacksonLayout(
    val enabled: Boolean
)

data class KotlinDtoBuilderLayout(
    val enabled: Boolean,
    val decoration: Decoration
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
    val onDecoration: Decoration
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
    val generics: List<KotlinType> = listOf()
) : Serializable {
    val packageName: String = packageName.validateKotlinPath()

    val className: String = className.validateKotlinPath()

    fun nullable(): KotlinType =
        if (allowNull) this else KotlinType(packageName, className, true, generics)

    fun parameterize(vararg arguments: KotlinType): KotlinType =
        if (arguments.isEmpty()) this else KotlinType(packageName, className, allowNull, generics + arguments)

    fun nested(nestedClassName: String): KotlinType =
        KotlinType(packageName, "$className.$nestedClassName", allowNull, generics)


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
