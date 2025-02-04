package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.*
import io.github.ermadmi78.kobby.model.KobbyNodeKind.INPUT

fun KobbySchema.truncate(
    regexEnabled: Boolean = false,
    caseSensitive: Boolean = true,
    query: KobbySchemaQueryScope.() -> Unit
) = truncate(
    KobbySchemaQueryScope(this, regexEnabled, caseSensitive)
        .apply(query)
        ._buildPredicate()
)

/**
 * Truncates GraphQL schema with the specified type predicate. The type predicate consists of two
 * sub-predicates: field predicate and enum value predicate.
 *
 * The field predicate applies to fields of GraphQL objects, interfaces and inputs and returns a flag
 * indicating whether the field should be stored in the filtered schema type or not.
 *
 * The enum value predicate applies to enum values of GraphQL enums and returns a flag
 * indicating whether the enum value should be stored in the filtered schema type or not.
 *
 * After removing all fields end enum values that do not satisfy the filtering conditions,
 * the method analyzes the dependency graph of GraphQL types and leaves only those types
 * that are reachable from the root of the schema - Query, Mutation, and Subscription types.
 *
 * @param predicate Type predicate.
 * @return filtered deep clone of the schema
 *
 * Created on 04.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
fun KobbySchema.truncate(predicate: KobbyTypePredicate): KobbySchema {
    val fieldPredicate: (KobbyField) -> Boolean = predicate.first
    val enumValuePredicate: (KobbyEnumValue) -> Boolean = predicate.second

    // Node name → set of excluded node fields.
    val availableNodeToExcludedFieldNamesMap = mutableMapOf<KobbyNode, Set<String>>()

    // Available supertypes
    val supertypes = mutableSetOf<KobbyNode>()

    // Enum node → set of required enum values.
    val availableEnumToRequiredValueMap = mutableMapOf<KobbyNode, MutableSet<KobbyEnumValue>>()

    query.scanDependencyGraph(
        availableNodeToExcludedFieldNamesMap,
        supertypes,
        availableEnumToRequiredValueMap,
        null,
        fieldPredicate
    )
    mutation.scanDependencyGraph(
        availableNodeToExcludedFieldNamesMap,
        supertypes,
        availableEnumToRequiredValueMap,
        null,
        fieldPredicate
    )
    subscription.scanDependencyGraph(
        availableNodeToExcludedFieldNamesMap,
        supertypes,
        availableEnumToRequiredValueMap,
        null,
        fieldPredicate
    )

    for (node in supertypes) {
        if (node in availableNodeToExcludedFieldNamesMap) {
            continue
        }

        val excludeFields = mutableSetOf<String>()
        availableNodeToExcludedFieldNamesMap[node] = excludeFields

        node.fields { field ->
            if (!field.match(fieldPredicate)) {
                excludeFields += field.name
            }
        }
    }

    availableEnumToRequiredValueMap.forEach { (enumNode, requiredEnumValues) ->
        val excludeFields = mutableSetOf<String>()
        enumNode.enumValues { enumValue ->
            if ((enumValue !in requiredEnumValues) && !enumValuePredicate(enumValue)) {
                excludeFields += enumValue.name
            }
        }
        availableNodeToExcludedFieldNamesMap[enumNode] = excludeFields
    }

    val sourceSchema = this
    return KobbySchema {
        sourceSchema.operations.forEach { (operation, name) ->
            addOperation(operation, name)
        }

        for (sourceNode in sourceSchema.all.values) {
            val excludeFields = availableNodeToExcludedFieldNamesMap[sourceNode] ?: continue
            addNode(sourceNode.name, sourceNode.kind) {
                clone(sourceNode, excludeFields)
            }
        }
    }
}

//**********************************************************************************************************************
//                                                Filtering
//**********************************************************************************************************************

/**
 * @param availableNodeToExcludedFieldNamesMap Node → set of excluded node fields.
 * @param supertypes Available supertypes
 * @param availableEnumToRequiredValueMap Enum node → set of required enum values.
 * @param defaultValue Default value of enum
 * @param fieldPredicate Filter predicate. Type name, field name → whether to include the field in the resulting schema.
 */
private fun KobbyNode.scanDependencyGraph(
    availableNodeToExcludedFieldNamesMap: MutableMap<KobbyNode, Set<String>>,
    supertypes: MutableSet<KobbyNode>,
    availableEnumToRequiredValueMap: MutableMap<KobbyNode, MutableSet<KobbyEnumValue>>,
    defaultValue: KobbyLiteral?,
    fieldPredicate: (KobbyField) -> Boolean
) {
    if (kind == KobbyNodeKind.ENUM) {
        val requiredEnumValues = availableEnumToRequiredValueMap.getOrPut(this) {
            mutableSetOf()
        }
        if (defaultValue is KobbyEnumLiteral) {
            enumValues[defaultValue.name]?.also {
                requiredEnumValues += it
            }
        }
        return // Enums will be stored in availabilityMap separately
    }

    if (this in availableNodeToExcludedFieldNamesMap) {
        return // Cutting off the dependency cycle
    }

    val excludeFields = mutableSetOf<String>()
    availableNodeToExcludedFieldNamesMap[this] = excludeFields

    fields { field ->
        if (field.match(fieldPredicate)) {
            val fieldReturnNode = field.type.node
            fieldReturnNode.scanDependencyGraph(
                availableNodeToExcludedFieldNamesMap,
                supertypes,
                availableEnumToRequiredValueMap,
                field.defaultValue,
                fieldPredicate
            )
            fieldReturnNode.subTree.forEach { subObject ->
                subObject.scanDependencyGraph(
                    availableNodeToExcludedFieldNamesMap,
                    supertypes,
                    availableEnumToRequiredValueMap,
                    null,
                    fieldPredicate
                )
            }

            field.arguments { argument ->
                argument.type.node.scanDependencyGraph(
                    availableNodeToExcludedFieldNamesMap,
                    supertypes,
                    availableEnumToRequiredValueMap,
                    argument.defaultValue,
                    fieldPredicate
                )
            }
        } else {
            excludeFields += field.name
        }
    }

    implements { parentObject ->
        supertypes += parentObject
    }
}

private fun KobbyField.match(predicate: (KobbyField) -> Boolean): Boolean {
    if (node.kind == INPUT) {
        return (!type.nullable && !hasDefaultValue) || predicate(this)
    }

    if (!isOverride) {
        return predicate(this)
    }

    var rootFieldsCount = 0
    for (parentNode in node.implements) {
        val parentField = parentNode.fields[name]
        if (parentField == null || parentField.isOverride) {
            continue
        }

        rootFieldsCount++
        if (predicate(parentField)) {
            return true
        }
    }

    require(rootFieldsCount > 0) { "Invalid algorithm" }
    return false
}

//**********************************************************************************************************************
//                                                Cloning
//**********************************************************************************************************************

private fun KobbyNodeScope.clone(sourceNode: KobbyNode, excludeFields: Set<String>) {
    sourceNode.implements { parentNode ->
        addImplements(parentNode.name)
    }
    sourceNode.comments { comment ->
        addComment(comment)
    }
    sourceNode.enumValues { sourceEnumValue ->
        if (sourceEnumValue.name !in excludeFields) {
            addEnumValue(sourceEnumValue.name) {
                sourceEnumValue.comments { comment ->
                    addComment(comment)
                }
            }
        }
    }
    sourceNode.fields { sourceField ->
        if (sourceField.name !in excludeFields) {
            addField(
                sourceField.name,
                sourceField.type.clone(schema),
                sourceField.defaultValue,
                sourceField.primaryKey,
                sourceField.required,
                sourceField.default,
                sourceField.selection
            ) {
                sourceField._comments.forEach { comment ->
                    addComment(comment)
                }
                sourceField.arguments { sourceArgument ->
                    addArgument(
                        sourceArgument.name,
                        sourceArgument.type.clone(schema),
                        sourceArgument.defaultValue
                    ) {
                        sourceArgument._comments.forEach { comment ->
                            addComment(comment)
                        }
                    }
                }
            }
        }
    }
}

private fun KobbyType.clone(targetSchema: KobbySchema): KobbyType = when (this) {
    is KobbyListType -> KobbyListType(nested.clone(targetSchema), nullable)
    is KobbyNodeType -> KobbyNodeType(targetSchema, nodeName, nullable)
}
