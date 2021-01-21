package io.kobby.model

import graphql.language.*
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kobby.model.KobbyDirective.Companion.RENAME_ARGUMENT
import java.io.Reader
import kotlin.LazyThreadSafetyMode.PUBLICATION

/**
 * Created on 19.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun parseSchema(directive: KobbyDirective, vararg schemas: Reader): KobbySchema = TypeDefinitionRegistry().also {
    for (schema in schemas) {
        it.merge(SchemaParser().parse(schema))
    }
}.toRegistryScope(directive).parseSchemaImpl()

private fun RegistryScope.parseSchemaImpl() = KobbySchema {
    val applyComments: KobbyNodeScope.(TypeDefinition<*>) -> Unit = { type ->
        type.comments.forEach {
            addComment(it.content)
        }
        nodeRenameConflicts[type.name]?.also { conflicts ->
            conflicts.asSequence().map { it.resolveType() }.filterNotNull().forEach {
                val message = "${type.title} conflicts with ${it.title}" //todo warning
                addComment("TODO $message")
            }
        }
    }

    scalars.values.forEach { scalar ->
        addScalar(scalar.effectiveName, scalar.name) {
            applyComments(scalar)
        }
    }

    types.values.forEach { type ->
        when (type) {
            is ObjectTypeDefinition -> addObject(type.effectiveName, type.name) {
                applyComments(type)
                type.implements.asSequence()
                    .map { it.extractName() }
                    .map { it.resolveType() }
                    .filterNotNull()
                    .filter { it is InterfaceTypeDefinition }
                    .forEach {
                        addImplements(it.effectiveName)
                    }
            }
        }
    }
}

private class RegistryScope(
    val directive: KobbyDirective,
    val types: Map<String, TypeDefinition<*>>,
    val scalars: Map<String, TypeDefinition<*>>,
    val directives: Map<String, DirectiveDefinition>,
    val scalarExtensions: Map<String, List<ScalarTypeExtensionDefinition>>,
    val objectExtensions: Map<String, List<ObjectTypeExtensionDefinition>>,
    val interfaceExtensions: Map<String, List<InterfaceTypeExtensionDefinition>>,
    val unionExtensions: Map<String, List<UnionTypeExtensionDefinition>>,
    val enumExtensions: Map<String, List<EnumTypeExtensionDefinition>>,
    val inputObjectExtensions: Map<String, List<InputObjectTypeExtensionDefinition>>,
) {
    val nodeRenameConflicts: Map<String, Set<String>> by lazy(PUBLICATION) {
        mutableMapOf<String, MutableSet<String>>().also { conflicts ->
            val renamedTypes = mutableMapOf<String, TypeDefinition<*>>()
            sequence { yieldAll(scalars.values); yieldAll(types.values) }.forEach { type ->
                renamedTypes.put(type.renamed, type)?.also {
                    conflicts.append(type.name, it.name)
                    conflicts.append(it.name, type.name)
                }
            }
        }
    }

    fun String.resolveType(): TypeDefinition<*>? =
        types[this] ?: scalars[this]

    fun TypeDefinition<*>.allDirectives(): Sequence<Directive> = sequence {
        yieldAll(directives)
        when (this) {
            is ScalarTypeDefinition -> scalarExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
            is ObjectTypeDefinition -> objectExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
            is InterfaceTypeDefinition -> interfaceExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
            is UnionTypeDefinition -> unionExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
            is EnumTypeDefinition -> enumExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
            is InputObjectTypeDefinition -> inputObjectExtensions[name]?.forEach {
                yieldAll(it.directives)
            }
        }
    }

    val Sequence<Directive>.renamed: String?
        get() = filter { it.name == directive.rename && it.arguments.size == 1 }
            .map { it.arguments.first() }
            .filter { it.name == RENAME_ARGUMENT && it.value is StringValue }
            .map { (it.value as StringValue).value?.trim() }
            .filterNotNull()
            .firstOrNull { it.isIdentifier() }

    fun ObjectTypeDefinition.allFields(): Sequence<FieldDefinition> = sequence {
        yieldAll(fieldDefinitions)
        objectExtensions[name]?.forEach {
            yieldAll(it.fieldDefinitions)
        }
    }

    fun InterfaceTypeDefinition.allFields(): Sequence<FieldDefinition> = sequence {
        yieldAll(fieldDefinitions)
        interfaceExtensions[name]?.forEach {
            yieldAll(it.fieldDefinitions)
        }
    }

    fun FieldDefinition.allInputValues(): Sequence<InputValueDefinition> = inputValueDefinitions.asSequence()

    val FieldDefinition.renamed: String
        get() = directives.asSequence().renamed ?: name

    val FieldDefinition.title: String
        get() = renamed.let {
            if (name == it) name else "$name renamed to $it"
        }

    fun Sequence<FieldDefinition>.findRenameConflicts(): Map<String, Set<String>> {
        val conflicts = mutableMapOf<String, MutableSet<String>>()
        val renamedFields = mutableMapOf<String, FieldDefinition>()
        forEach { field ->
            renamedFields.put(field.renamed, field)?.also {
                conflicts.append(field.name, it.name)
                conflicts.append(it.name, field.name)
            }
        }

        return conflicts
    }

    fun EnumTypeDefinition.allEnumValues(): Sequence<EnumValueDefinition> = sequence {
        yieldAll(enumValueDefinitions)
        enumExtensions[name]?.forEach {
            yieldAll(it.enumValueDefinitions)
        }
    }

    fun InputObjectTypeDefinition.allInputValues(): Sequence<InputValueDefinition> = sequence {
        yieldAll(inputValueDefinitions)
        inputObjectExtensions[name]?.forEach {
            yieldAll(it.inputValueDefinitions)
        }
    }

    val TypeDefinition<*>.renamed: String
        get() = allDirectives().renamed ?: name

    val TypeDefinition<*>.title: String
        get() = renamed.let {
            if (name == it) "$kind $name" else "$kind $name renamed to $it"
        }

    val TypeDefinition<*>.effectiveName: String
        get() = if (nodeRenameConflicts.isEmpty()) renamed else name

    fun Type<*>.resolve(schema: KobbySchema, nullable: Boolean = true): KobbyType = when (this) {
        is NonNullType -> type.resolve(schema, false)
        is ListType -> KobbyListType(type.resolve(schema), nullable)
        is TypeName -> KobbyNodeType(schema, name.resolveType()!!.effectiveName, nullable)
        else -> error("Unexpected Type successor: ${this::javaClass.name}")
    }
}

private fun TypeDefinitionRegistry.toRegistryScope(directive: KobbyDirective): RegistryScope = RegistryScope(
    directive,
    types(),
    scalars(),
    directiveDefinitions,
    scalarTypeExtensions(),
    objectTypeExtensions(),
    interfaceTypeExtensions(),
    unionTypeExtensions(),
    enumTypeExtensions(),
    inputObjectTypeExtensions()
)

private val TypeDefinition<*>.kind: String
    get() = when (this) {
        is ScalarTypeDefinition -> "scalar"
        is ObjectTypeDefinition -> "type"
        is InterfaceTypeDefinition -> "interface"
        is UnionTypeDefinition -> "union"
        is EnumTypeDefinition -> "enum"
        is InputObjectTypeDefinition -> "input"
        else -> "unknown"
    }

private fun <K : Any, V : Any> MutableMap<K, MutableSet<V>>.append(key: K, value: V) =
    computeIfAbsent(key) { mutableSetOf() }.add(value)

private fun Type<*>.extractName(): String = when (this) {
    is NonNullType -> type.extractName()
    is ListType -> type.extractName()
    is TypeName -> name
    else -> error("Unexpected Type successor: ${this::javaClass.name}")
}