package io.kobby.model

import graphql.language.*
import graphql.schema.idl.SchemaParser
import graphql.schema.idl.TypeDefinitionRegistry
import io.kobby.model.KobbyDirective.Companion.RENAME_ARGUMENT
import java.io.Reader
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Created on 19.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun parseSchema(directive: KobbyDirective, vararg schemas: Reader): KobbySchema {
    val graphQLSchema = TypeDefinitionRegistry()
    for (schema in schemas) {
        graphQLSchema.merge(SchemaParser().parse(schema))
    }

    //******************************************************************************************************************
    val all = mutableMapOf<String, KobbyNode>()
    val scalars = mutableMapOf<String, KobbyNode>()
    val query = arrayOfNulls<KobbyNode>(1)
    val mutation = arrayOfNulls<KobbyNode>(1)
    val objects = mutableMapOf<String, KobbyNode>()
    val interfaces = mutableMapOf<String, KobbyNode>()
    val unions = mutableMapOf<String, KobbyNode>()
    val enums = mutableMapOf<String, KobbyNode>()
    val inputs = mutableMapOf<String, KobbyNode>()

    val schema = KobbySchema(
        all,
        scalars,
        lazy(NONE) { query[0]!! },
        lazy(NONE) { mutation[0]!! },
        objects,
        interfaces,
        unions,
        enums,
        inputs
    )
    //******************************************************************************************************************

    return schema
}

private fun Registry.findNodeRenameConflicts(): Map<String, String> {
    val conflicts = mutableMapOf<String, String>()
    val renamedTypes = mutableMapOf<String, TypeDefinition<*>>()
    (scalars.values.asSequence() + types.values.asSequence()).forEach { type1 ->
        renamedTypes.put(type1.renamed, type1)?.also { type2 ->
            conflicts[type1.name] = "TODO ${type1.title} conflicts with ${type2.title}"
            conflicts[type2.name] = "TODO ${type2.title} conflicts with ${type1.title}"
        }
    }

    return conflicts
}

private data class Registry(
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
    fun TypeDefinition<*>.allDirectives() = sequence<Directive> {
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

    val TypeDefinition<*>.renamed: String
        get() = allDirectives()
            .filter { it.name == directive.rename && it.arguments.size == 1 }
            .map { it.arguments.first() }
            .filter { it.name == RENAME_ARGUMENT && it.value is StringValue }
            .map { (it.value as StringValue).value?.trim() }
            .filterNotNull()
            .firstOrNull { it.isIdentifier() }
            ?: name

    val TypeDefinition<*>.title: String
        get() = renamed.let {
            if (name == it) "$kind $name" else "$kind $name renamed to $it"
        }
}

private fun TypeDefinitionRegistry.toRegistry(directive: KobbyDirective): Registry = Registry(
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
