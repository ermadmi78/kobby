package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 25.04.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

val PREDEFINED_SCALARS: Set<String> = setOf(
    "ID", "Int", "Long", "Float", "Double", "String", "Boolean"
)

class TruncationReport(
    private val source: KobbySchema,
    private val truncated: KobbySchema
) {
    private val sourceScalars: Map<String, KobbyNode> = source.scalars - PREDEFINED_SCALARS
    private val truncatedScalars: Map<String, KobbyNode> = truncated.scalars - PREDEFINED_SCALARS

    private val excludedScalarsCount: Int = sourceScalars.size - truncatedScalars.size
    private val excludedObjectsCount: Int = source.objects.size - truncated.objects.size
    private val excludedInterfacesCount: Int = source.interfaces.size - truncated.interfaces.size
    private val excludedUnionsCount: Int = source.unions.size - truncated.unions.size
    private val excludedEnumsCount: Int = source.enums.size - truncated.enums.size
    private val excludedInputsCount: Int = source.inputs.size - truncated.inputs.size

    init {
        require(excludedScalarsCount >= 0) { "Invalid algorithm (excludedScalarsCount < 0)" }
        require(excludedObjectsCount >= 0) { "Invalid algorithm (excludedObjectsCount < 0)" }
        require(excludedInterfacesCount >= 0) { "Invalid algorithm (excludedInterfacesCount < 0)" }
        require(excludedUnionsCount >= 0) { "Invalid algorithm (excludedUnionsCount < 0)" }
        require(excludedEnumsCount >= 0) { "Invalid algorithm (excludedEnumsCount < 0)" }
        require(excludedInputsCount >= 0) { "Invalid algorithm (excludedInputsCount < 0)" }
    }

    // without scalars
    private val excludedTotalCount: Int = excludedObjectsCount +
            excludedInterfacesCount +
            excludedUnionsCount +
            excludedEnumsCount +
            excludedInputsCount

    fun truncationCompletedStr(prefix: String): String {
        return if (excludedTotalCount == 0) {
            "$prefix GraphQL schema truncation completed. No GraphQL types excluded."
        } else {
            "$prefix GraphQL schema truncation completed. " +
                    "$excludedTotalCount GraphQL types excluded " +
                    reportTypesCounterStr(
                        excludedObjectsCount,
                        excludedInterfacesCount,
                        excludedUnionsCount,
                        excludedEnumsCount,
                        excludedInputsCount
                    )
        }
    }

    fun detailedReport(): Sequence<String> = sequence {
        if (excludedScalarsCount > 0) {
            yield("")
            yield("Excluded scalars:")
            printExcludedScalars()
        }

        if (excludedObjectsCount > 0) {
            yield("")
            yield("Excluded objects:")
            printExcludedTypes { objects }
        }

        if (excludedInterfacesCount > 0) {
            yield("")
            yield("Excluded interfaces:")
            printExcludedTypes { interfaces }
        }

        if (excludedUnionsCount > 0) {
            yield("")
            yield("Excluded unions:")
            printExcludedTypes { unions }
        }

        if (excludedEnumsCount > 0) {
            yield("")
            yield("Excluded enums:")
            printExcludedTypes { enums }
        }

        if (excludedInputsCount > 0) {
            yield("")
            yield("Excluded inputs:")
            printExcludedTypes { inputs }
        }

        printExcludedTypeFields("object") { objects }
        printExcludedTypeFields("interface") { interfaces }
        printExcludedEnumValues()
    }

    private suspend fun SequenceScope<String>.printExcludedScalars() {
        val truncated: Set<String> = sourceScalars.keys - truncatedScalars.keys
        for (type in truncated) {
            yield("    $type")
        }
    }

    private suspend fun SequenceScope<String>.printExcludedTypes(actual: KobbySchema.() -> Map<String, KobbyNode>) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        val truncated: Set<String> = actualSourceTypes.keys - actualTruncatedTypes.keys
        for (type in truncated) {
            yield("  $type")
        }
    }

    private suspend fun SequenceScope<String>.printExcludedTypeFields(
        typeKind: String,
        actual: KobbySchema.() -> Map<String, KobbyNode>
    ) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        var first = true
        actualTruncatedTypes.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = actualSourceTypes[truncatedNode.name] ?: truncatedNode
                sourceNode.name to sourceNode.fields.keys - truncatedNode.fields.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .forEach { (typeName: String, fieldNames: Set<String>) ->
                if (first) {
                    first = false
                    yield("")
                    yield("Excluded $typeKind fields:")
                }

                yield("")
                yield("  ${typeName}:")
                for (fieldName in fieldNames) {
                    yield("    $fieldName")
                }
            }
    }

    private suspend fun SequenceScope<String>.printExcludedEnumValues() {
        var first = true
        truncated.enums.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = source.enums[truncatedNode.name] ?: truncatedNode
                sourceNode.name to sourceNode.enumValues.keys - truncatedNode.enumValues.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .forEach { (typeName: String, fieldNames: Set<String>) ->
                if (first) {
                    first = false
                    yield("")
                    yield("Excluded enum values:")
                }

                yield("  ${typeName}:")
                for (fieldName in fieldNames) {
                    yield("    $fieldName")
                }
            }
    }
}

fun reportQueryPropertiesStr(
    regexEnabled: Boolean,
    caseSensitive: Boolean
): String {
    return " (" +
            (if (regexEnabled) "Regex" else "Kobby pattern") +
            (if (caseSensitive) " case sensitive" else " case insensitive") +
            ")"
}

fun KobbySchema.reportWeightStr(prefix: String): String {
    val objectsCount = this.objects.size
    val interfacesCount = this.interfaces.size
    val unionsCount = this.unions.size
    val enumsCount = this.enums.size
    val inputsCount = this.inputs.size

    val totalCount = objectsCount +
            interfacesCount +
            unionsCount +
            enumsCount +
            inputsCount

    return if (totalCount == 0) {
        "$prefix is empty."
    } else {
        "$prefix weight: $totalCount " +
                reportTypesCounterStr(
                    objectsCount,
                    interfacesCount,
                    unionsCount,
                    enumsCount,
                    inputsCount
                )
    }
}

private fun reportTypesCounterStr(
    objectsCount: Int,
    interfacesCount: Int,
    unionsCount: Int,
    enumsCount: Int,
    inputsCount: Int
): String = buildString {
    append('(')

    var counter = 0
    if (objectsCount != 0) {
        counter++
        append(objectsCount).append(" objects")
    }
    if (interfacesCount != 0) {
        if (counter++ > 0) {
            append(", ")
        }
        append(interfacesCount).append(" interfaces")
    }
    if (unionsCount != 0) {
        if (counter++ > 0) {
            append(", ")
        }
        append(unionsCount).append(" unions")
    }
    if (enumsCount != 0) {
        if (counter++ > 0) {
            append(", ")
        }
        append(enumsCount).append(" enums")
    }
    if (inputsCount != 0) {
        if (counter > 0) {
            append(", ")
        }
        append(inputsCount).append(" inputs")
    }

    append(')').append('.')
}