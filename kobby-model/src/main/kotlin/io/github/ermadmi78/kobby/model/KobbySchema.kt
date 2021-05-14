package io.github.ermadmi78.kobby.model

import io.github.ermadmi78.kobby.model.KobbyNodeKind.*
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbySchema internal constructor(
    val operations: Map<Operation, String>, // operation to node name
    val all: Map<String, KobbyNode>,
    val scalars: Map<String, KobbyNode>,
    val objects: Map<String, KobbyNode>,
    val interfaces: Map<String, KobbyNode>,
    val unions: Map<String, KobbyNode>,
    val enums: Map<String, KobbyNode>,
    val inputs: Map<String, KobbyNode>,

    // name of interface or union -> set of names of object or interface
    val subObjectsIndex: Map<String, Set<String>>
) {
    val query: KobbyNode by lazy(NONE) { objects[operations[Operation.QUERY]!!]!! }
    val mutation: KobbyNode by lazy(NONE) { objects[operations[Operation.MUTATION]!!]!! }

    fun all(action: (KobbyNode) -> Unit) = all.values.forEach(action)
    fun scalars(action: (KobbyNode) -> Unit) = scalars.values.forEach(action)
    fun objects(action: (KobbyNode) -> Unit) = objects.values.forEach(action)
    fun interfaces(action: (KobbyNode) -> Unit) = interfaces.values.forEach(action)
    fun unions(action: (KobbyNode) -> Unit) = unions.values.forEach(action)
    fun enums(action: (KobbyNode) -> Unit) = enums.values.forEach(action)
    fun inputs(action: (KobbyNode) -> Unit) = inputs.values.forEach(action)
}

fun KobbySchema(block: KobbySchemaScope.() -> Unit): KobbySchema =
    KobbySchemaScope().apply(block).build()

@KobbyScope
class KobbySchemaScope internal constructor() {
    private val operations = mutableMapOf<Operation, String>().also {
        Operation.values().forEach { operation ->
            it[operation] = operation.defaultNodeName
        }
    }
    private val all = mutableMapOf<String, KobbyNode>()
    private val scalars = mutableMapOf<String, KobbyNode>()
    private val objects = mutableMapOf<String, KobbyNode>()
    private val interfaces = mutableMapOf<String, KobbyNode>()
    private val unions = mutableMapOf<String, KobbyNode>()
    private val enums = mutableMapOf<String, KobbyNode>()
    private val inputs = mutableMapOf<String, KobbyNode>()
    private val subObjectsIndex = mutableMapOf<String, MutableSet<String>>()

    val schema = KobbySchema(
        operations,
        all,
        scalars,
        objects,
        interfaces,
        unions,
        enums,
        inputs,
        subObjectsIndex
    )

    fun addOperation(operation: Operation, nodeName: String) {
        operations[operation] = nodeName
    }

    fun addNode(
        name: String,
        kind: KobbyNodeKind,
        block: KobbyNodeScope.() -> Unit = {}
    ) = KobbyNodeScope(schema, name, kind).apply(block).build().also { node ->
        all[node.name] = node
        when (node.kind) {
            SCALAR -> scalars[node.name] = node
            OBJECT -> {
                objects[node.name] = node
                node._implements.forEach {
                    subObjectsIndex.append(it, node.name)
                }
            }
            INTERFACE -> {
                interfaces[node.name] = node
                node._implements.forEach {
                    subObjectsIndex.append(it, node.name)
                }
            }
            UNION -> unions[node.name] = node
            ENUM -> enums[node.name] = node
            INPUT -> inputs[node.name] = node
        }
    }

    fun addScalar(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, SCALAR, block)

    fun addObject(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, OBJECT, block)

    fun addInterface(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, INTERFACE, block)

    fun addUnion(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, UNION, block)

    fun addEnum(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, ENUM, block)

    fun addInput(name: String, block: KobbyNodeScope.() -> Unit = {}) =
        addNode(name, INPUT, block)

    private fun Operation.buildDefaultIfNeed() {
        operations[this]!!.takeIf { it !in objects }?.also {
            addObject(it)
        }
    }

    fun build(): KobbySchema {
        // Check operation node names
        mutableSetOf<String>().also { names ->
            operations.values.forEach {
                if (!names.add(it)) {
                    throw IllegalStateException("Duplicate operation node name: '$it'")
                }
            }
        }

        Operation.QUERY.buildDefaultIfNeed()
        Operation.MUTATION.buildDefaultIfNeed()

        return schema
    }
}