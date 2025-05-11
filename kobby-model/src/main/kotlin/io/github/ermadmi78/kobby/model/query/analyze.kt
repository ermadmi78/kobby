package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbyEnumValue
import io.github.ermadmi78.kobby.model.KobbyField
import io.github.ermadmi78.kobby.model.KobbyNode
import io.github.ermadmi78.kobby.model.KobbyNodeKind.*
import io.github.ermadmi78.kobby.model.KobbySchema

/**
 * Created on 22.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

sealed class PathElement {
    abstract val node: KobbyNode
}

class NodePathElement(
    override val node: KobbyNode
) : PathElement()

class FieldPathElement(
    val field: KobbyField
) : PathElement() {
    override val node: KobbyNode
        get() = this.field.type.node
}

class EnumValuePathElement(
    val enumValue: KobbyEnumValue
) : PathElement() {
    override val node: KobbyNode
        get() = this.enumValue.node
}

class Path(
    val elements: List<PathElement>
) : List<PathElement> by elements {
    constructor(node: KobbyNode) : this(listOf(NodePathElement(node)))

    init {
        require(elements.isNotEmpty()) { "Path must not be empty" }
    }

    val last: PathElement
        get() = last()

    operator fun contains(node: KobbyNode): Boolean {
        for (element in elements) {
            if (node == element.node) {
                return true
            }
        }

        return false
    }

    operator fun plus(node: KobbyNode): Path =
        Path(elements + NodePathElement(node))

    operator fun plus(field: KobbyField): Path =
        Path(elements + FieldPathElement(field))

    operator fun plus(enumValue: KobbyEnumValue): Path =
        Path(elements + EnumValuePathElement(enumValue))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Path
        return elements == other.elements
    }

    override fun hashCode(): Int {
        return elements.hashCode()
    }

    fun toReportString(
        withMinWeight: Int,
        withOverride: Boolean,
        withArgumentTypes: Boolean,
        withSuperTypes: Boolean,
        withSubTypes: Boolean
    ): String = buildString {
        val element: PathElement = last
        repeat(size - 1) {
            append(' ').append(' ')
        }

        when (element) {
            is NodePathElement -> {
                val node = element.node

                append(node.name)
                printMinWeight(node, withMinWeight)

                if (withSuperTypes) {
                    printSuperTypes(node)
                }
                if (withSubTypes) {
                    printSubTypes(node)
                }
            }

            is FieldPathElement -> {
                val field = element.field
                append(field.name)

                if (withArgumentTypes) {
                    printArgumentTypes(field)
                }

                if (withOverride) {
                    printOverride(field)
                }

                append(": ").append(field.type.node.name)
                printMinWeight(field, withMinWeight)

                if (withSuperTypes) {
                    printSuperTypes(field.type.node)
                }
                if (withSubTypes) {
                    printSubTypes(field.type.node)
                }
            }

            is EnumValuePathElement -> {
                append(element.enumValue.name)
            }
        }
    }
}

private fun StringBuilder.printArgumentTypes(field: KobbyField) {
    var first = true
    for (arg in field.argumentDependencies) {
        if (first) {
            first = false
            append('(')
        } else {
            append(", ")
        }

        append(arg.name)
    }
    if (!first) {
        append(')')
    }
}

private fun StringBuilder.printOverride(field: KobbyField) {
    if (field.isOverride) {
        append('^')
    }
}

private fun StringBuilder.printMinWeight(node: KobbyNode, minWeight: Int) {
    if (minWeight < 0) {
        return
    }

    val weight = node.weight
    if (weight >= minWeight) {
        append(" (weight: ").append(weight).append(')')
    }
}

private fun StringBuilder.printMinWeight(field: KobbyField, minWeight: Int) {
    if (minWeight < 0) {
        return
    }

    val weight = field.weight
    if (weight >= minWeight) {
        append(" (weight: ").append(weight).append(')')
    }
}

private fun StringBuilder.printSuperTypes(node: KobbyNode) {
    var counter = 0
    node.implements { superType ->
        append(if (counter++ == 0) " <- " else ", ")
        append(superType.name)
    }
}

private fun StringBuilder.printSubTypes(node: KobbyNode) {
    var counter = 0
    node.subTree.forEach { subType ->
        append(if (counter++ == 0) " -> " else ", ")
        append(subType.name)
    }
}

fun KobbySchema.analyze(
    depth: Int = 1,
    regexEnabled: Boolean = false,
    caseSensitive: Boolean = true,
    query: KobbySchemaQueryScope.() -> Unit
): Sequence<Path> {
    if (depth == 0) {
        return emptySequence()
    }

    val root: Map<KobbyNode, KobbyTypePredicate> = KobbySchemaQueryScope(this, regexEnabled, caseSensitive)
        .apply(query)
        ._affectBoth()
        ._buildMap()

    return sequence {
        for (entry in root) {
            var first = true
            val path = Path(entry.key)
            analyzeChildren(entry.value, path, depth) {
                if (first) {
                    first = false
                    yield(path)
                }
            }
        }
    }
}

private suspend fun SequenceScope<Path>.analyzeChildren(
    predicate: KobbyTypePredicate,
    parentPath: Path,
    depth: Int,
    yieldParent: suspend SequenceScope<Path>.() -> Unit = {}
) {
    if (depth == 0) {
        return
    }

    val parentNode = parentPath.last.node
    when (parentNode.kind) {
        OBJECT, INPUT -> {
            val nextDepth = if (depth < 0) depth else depth - 1
            parentNode.fields { field ->
                if (predicate.first(field)) {
                    val path = parentPath + field
                    yieldParent()
                    yield(path)

                    if (path.last.node !in parentPath && nextDepth != 0) {
                        analyzeChildren(predicate, path, nextDepth)
                    }
                }
            }
        }

        INTERFACE, UNION -> {
            if (parentPath.size == 1) {
                if (depth == 1) {
                    parentNode.fields { field ->
                        if (predicate.first(field)) {
                            val path = parentPath + field
                            yieldParent()
                            yield(path)
                        }
                    }
                } else {
                    val nextDepth = if (depth < 0) depth else depth - 1
                    parentNode.children { node ->
                        var first = true
                        val path = parentPath + node
                        analyzeChildren(predicate, path, nextDepth) {
                            if (first) {
                                first = false
                                yieldParent()
                                yield(path)
                            }
                        }
                    }
                }
            } else {
                parentNode.children { node ->
                    var first = true
                    val path = parentPath + node
                    analyzeChildren(predicate, path, if (path.last.node in parentPath) 1 else depth) {
                        if (first) {
                            first = false
                            yieldParent()
                            yield(path)
                        }
                    }
                }
            }
        }

        ENUM -> {
            if (parentPath.size == 1) {
                parentNode.enumValues { enumValue ->
                    if (predicate.second(enumValue)) {
                        val path = parentPath + enumValue
                        yieldParent()
                        yield(path)
                    }
                }
            }
        }

        SCALAR -> {
            // Do nothing
        }
    }
}