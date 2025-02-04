package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.KobbyScope
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Created on 22.03.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

fun KobbySchema.assertTruncation(
    regexEnabled: Boolean = false,
    caseSensitive: Boolean = true,
    query: KobbySchemaQueryScope.() -> Unit
) = ActualTruncation(
    this,
    this.truncate(regexEnabled, caseSensitive, query)
)

class ActualTruncation(
    private val source: KobbySchema,
    private val truncated: KobbySchema
) {
    fun shouldExclude(scope: TruncationExcludeExpectationScope.() -> Unit) {
        val expect = TruncationExcludeExpectationScope()
            .apply(scope)
            ._build()

        types(expect.scalars, "scalars") { scalars - PREDEFINED_SCALARS }
        types(expect.objects, "objects") { objects }
        types(expect.interfaces, "interfaces") { interfaces }
        types(expect.unions, "unions") { unions }
        types(expect.enums, "enums") { enums }
        types(expect.inputs, "inputs") { inputs }

        typeFields(expect.objectFields, "object") { objects }
        typeFields(expect.interfaceFields, "interface") { interfaces }

        enumValues(expect.enumValues)
    }

    private fun types(
        expectedToBeExcluded: Set<String>,
        types: String,
        actual: KobbySchema.() -> Map<String, KobbyNode>
    ) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        actualTruncatedTypes.values.asSequence()
            .map { it.name }
            .filter { it !in actualSourceTypes }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid truncation algorithm. " +
                            "Truncated schema contains $types that are not present in the source schema: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .filter { it !in actualSourceTypes }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid expectation. " +
                            "Source schema does not contain $types that are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .filter { it in actualTruncatedTypes }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("Truncated schema contains $types that are expected to be excluded: $it")
            }

        actualSourceTypes.values.asSequence()
            .map { it.name }
            .filter { it !in actualTruncatedTypes }
            .filter { it !in expectedToBeExcluded }
            .map { "\"${it}\"" }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("There are unexpected excluded $types: $it")
            }
    }

    private fun typeFields(
        expectedToBeExcluded: Map<String, Set<String>>,
        type: String,
        actual: KobbySchema.() -> Map<String, KobbyNode>
    ) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        actualTruncatedTypes.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = actualSourceTypes[truncatedNode.name] ?: fail("Invalid algorithm")
                val shouldBeEmpty: Set<String> = truncatedNode.fields.keys - sourceNode.fields.keys
                if (shouldBeEmpty.isEmpty()) "" else "${sourceNode.name}(${shouldBeEmpty.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid truncation algorithm. " +
                            "Truncated schema contains $type fields that are not present " +
                            "in the source schema $type fields: $it"
                )
            }

        expectedToBeExcluded.keys.asSequence()
            .filter { it !in actualSourceTypes }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid expectation. " +
                            "Source schema does not contain ${type}s whose fields are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val sourceNode = actualSourceTypes[typeName] ?: fail("Invalid algorithm")
                val shouldBeEmpty: Set<String> = fieldNames - sourceNode.fields.keys
                if (shouldBeEmpty.isEmpty()) "" else "${sourceNode.name}(${shouldBeEmpty.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid expectation. " +
                            "Source schema $type fields does not contain fields that are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.keys.asSequence()
            .filter { it !in actualTruncatedTypes }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Truncated schema does not contain ${type}s whose fields are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val truncatedNode = actualTruncatedTypes[typeName] ?: fail("Invalid algorithm")
                typeName to fieldNames.intersect(truncatedNode.fields.keys)
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .map { (typeName: String, fieldNames: Set<String>) ->
                "${typeName}(${fieldNames.joinToString()})"
            }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("Truncated schema contains $type fields that are expected to be excluded: $it")
            }

        val actualExcluded: Map<String, Set<String>> = actualTruncatedTypes.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = actualSourceTypes[truncatedNode.name] ?: fail("Invalid algorithm")
                sourceNode.name to sourceNode.fields.keys - truncatedNode.fields.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .toMap()

        actualExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val expected: Set<String> = expectedToBeExcluded[typeName] ?: emptySet()
                val unexpected: Set<String> = fieldNames - expected
                if (unexpected.isEmpty()) "" else
                    "${typeName}(${unexpected.asSequence().map { "\"${it}\"" }.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("There are unexpected excluded $type fields: $it")
            }

        assertEquals(expectedToBeExcluded, actualExcluded, "Excluded $type fields is wrong")
    }

    private fun enumValues(expectedToBeExcluded: Map<String, Set<String>>) {
        truncated.enums.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = source.enums[truncatedNode.name] ?: fail("Invalid algorithm")
                val shouldBeEmpty: Set<String> = truncatedNode.enumValues.keys - sourceNode.enumValues.keys
                if (shouldBeEmpty.isEmpty()) "" else "${sourceNode.name}(${shouldBeEmpty.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid truncation algorithm. " +
                            "Truncated schema contains enum values that are not present " +
                            "in the source schema enum values: $it"
                )
            }

        expectedToBeExcluded.keys.asSequence()
            .filter { it !in source.enums }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid expectation. " +
                            "Source schema does not contain enums whose fields are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val sourceNode = source.enums[typeName] ?: fail("Invalid algorithm")
                val shouldBeEmpty: Set<String> = fieldNames - sourceNode.enumValues.keys
                if (shouldBeEmpty.isEmpty()) "" else "${sourceNode.name}(${shouldBeEmpty.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Invalid expectation. " +
                            "Source schema enum values does not contain fields that are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.keys.asSequence()
            .filter { it !in truncated.enums }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail(
                    "Truncated schema does not contain enums whose fields are expected to be excluded: $it"
                )
            }

        expectedToBeExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val truncatedNode = truncated.enums[typeName] ?: fail("Invalid algorithm")
                typeName to fieldNames.intersect(truncatedNode.enumValues.keys)
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .map { (typeName: String, fieldNames: Set<String>) ->
                "${typeName}(${fieldNames.joinToString()})"
            }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("Truncated schema contains enum values that are expected to be excluded: $it")
            }

        val actualExcluded: Map<String, Set<String>> = truncated.enums.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = source.enums[truncatedNode.name] ?: fail("Invalid algorithm")
                sourceNode.name to sourceNode.enumValues.keys - truncatedNode.enumValues.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .toMap()

        actualExcluded.asSequence()
            .map { (typeName: String, fieldNames: Set<String>) ->
                val expected: Set<String> = expectedToBeExcluded[typeName] ?: emptySet()
                val unexpected: Set<String> = fieldNames - expected
                if (unexpected.isEmpty()) "" else
                    "${typeName}(${unexpected.asSequence().map { "\"${it}\"" }.joinToString()})"
            }
            .filter { it.isNotEmpty() }
            .joinToString()
            .takeIf { it.isNotEmpty() }?.also {
                fail("There are unexpected excluded enum values: $it")
            }

        assertEquals(expectedToBeExcluded, actualExcluded, "Excluded enum values is wrong")
    }

    fun print(enabled: Boolean = true): ActualTruncation {
        if (!enabled) {
            return this
        }

        println()
        println("**********************************************************************")
        printTypes("scalars") { scalars - PREDEFINED_SCALARS }
        printTypes("objects") { objects }
        printTypes("interfaces") { interfaces }
        printTypes("unions") { unions }
        printTypes("enums") { enums }
        printTypes("inputs") { inputs }
        printTypeFields("objectFields") { objects }
        printTypeFields("interfaceFields") { interfaces }
        printEnumValues()
        println("**********************************************************************")
        return this
    }

    private fun printTypes(
        types: String,
        actual: KobbySchema.() -> Map<String, KobbyNode>
    ) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        val truncated: Set<String> = actualSourceTypes.keys - actualTruncatedTypes.keys
        if (truncated.isNotEmpty()) {
            println("${types}(${truncated.asSequence().map { "\"${it}\"" }.joinToString()})")
        }
    }

    private fun printTypeFields(
        fields: String,
        actual: KobbySchema.() -> Map<String, KobbyNode>
    ) {
        val actualSourceTypes: Map<String, KobbyNode> = actual(source)
        val actualTruncatedTypes: Map<String, KobbyNode> = actual(truncated)

        actualTruncatedTypes.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = actualSourceTypes[truncatedNode.name] ?: truncatedNode
                sourceNode.name to sourceNode.fields.keys - truncatedNode.fields.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .forEach { (typeName: String, fieldNames: Set<String>) ->
                println("${fields}(\"${typeName}\", ${fieldNames.asSequence().map { "\"${it}\"" }.joinToString()})")
            }
    }

    private fun printEnumValues() {
        truncated.enums.values.asSequence()
            .map { truncatedNode ->
                val sourceNode = source.enums[truncatedNode.name] ?: truncatedNode
                sourceNode.name to sourceNode.enumValues.keys - truncatedNode.enumValues.keys
            }
            .filter { (_: String, fieldNames: Set<String>) -> fieldNames.isNotEmpty() }
            .forEach { (typeName: String, fieldNames: Set<String>) ->
                println("enumValues(\"${typeName}\", ${fieldNames.asSequence().map { "\"${it}\"" }.joinToString()})")
            }
    }
}

class TruncationExcludeExpectation(
    val scalars: Set<String>,
    val objects: Set<String>,
    val interfaces: Set<String>,
    val unions: Set<String>,
    val enums: Set<String>,
    val inputs: Set<String>,

    val objectFields: Map<String, Set<String>>,
    val interfaceFields: Map<String, Set<String>>,
    val enumValues: Map<String, Set<String>>
)

@KobbyScope
class TruncationExcludeExpectationScope {
    private val scalars = mutableSetOf<String>()
    private val objects = mutableSetOf<String>()
    private val interfaces = mutableSetOf<String>()
    private val unions = mutableSetOf<String>()
    private val enums = mutableSetOf<String>()
    private val inputs = mutableSetOf<String>()

    private val objectFields = mutableMapOf<String, MutableSet<String>>()
    private val interfaceFields = mutableMapOf<String, MutableSet<String>>()
    private val enumValues = mutableMapOf<String, MutableSet<String>>()

    @Suppress("TestFunctionName")
    fun _build() = TruncationExcludeExpectation(
        scalars,
        objects,
        interfaces,
        unions,
        enums,
        inputs,
        objectFields.filter { (_, fields) -> fields.isNotEmpty() },
        interfaceFields.filter { (_, fields) -> fields.isNotEmpty() },
        enumValues.filter { (_, values) -> values.isNotEmpty() }
    )

    fun scalars(vararg expectations: String) {
        scalars += expectations
    }

    fun objects(vararg expectations: String) {
        objects += expectations
    }

    fun interfaces(vararg expectations: String) {
        interfaces += expectations
    }

    fun unions(vararg expectations: String) {
        unions += expectations
    }

    fun enums(vararg expectations: String) {
        enums += expectations
    }

    fun inputs(vararg expectations: String) {
        inputs += expectations
    }

    //**************************************************

    fun objectFields(name: String, vararg expectations: String) {
        objectFields.getOrPut(name) { mutableSetOf() } += expectations
    }

    fun interfaceFields(name: String, vararg expectations: String) {
        interfaceFields.getOrPut(name) { mutableSetOf() } += expectations
    }

    fun enumValues(name: String, vararg expectations: String) {
        enumValues.getOrPut(name) { mutableSetOf() } += expectations
    }
}