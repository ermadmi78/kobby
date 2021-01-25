package io.kobby.model

import io.kobby.model.KobbyNodeKind.*
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbySchema internal constructor(
    val all: Map<String, KobbyNode>,
    val scalars: Map<String, KobbyNode>,
    private val _query: Array<KobbyNode?>,
    private val _mutation: Array<KobbyNode?>,
    val objects: Map<String, KobbyNode>,
    val interfaces: Map<String, KobbyNode>,
    val unions: Map<String, KobbyNode>,
    val enums: Map<String, KobbyNode>,
    val inputs: Map<String, KobbyNode>,

    // name of interface or union -> set of names of object or interface
    val subObjectsIndex: Map<String, Set<String>>,

    // name of node -> set of fields from which node is returned
    val typeOfFieldsIndex: Map<String, Set<KobbyField>>
) {
    val query: KobbyNode by lazy(NONE) { _query[0]!! }
    val mutation: KobbyNode by lazy(NONE) { _mutation[0]!! }

    fun all(action: (KobbyNode) -> Unit) = all.values.forEach(action)
    fun scalars(action: (KobbyNode) -> Unit) = scalars.values.forEach(action)
    fun objects(action: (KobbyNode) -> Unit) = objects.values.forEach(action)
    fun interfaces(action: (KobbyNode) -> Unit) = interfaces.values.forEach(action)
    fun unions(action: (KobbyNode) -> Unit) = unions.values.forEach(action)
    fun enums(action: (KobbyNode) -> Unit) = enums.values.forEach(action)
    fun inputs(action: (KobbyNode) -> Unit) = inputs.values.forEach(action)

    fun objectsWithQueryAndMutation(action: (KobbyNode) -> Unit) = sequence {
        yield(query)
        yield(mutation)
        yieldAll(objects.values)
    }.forEach(action)
}

fun KobbySchema(block: KobbySchemaScope.() -> Unit): KobbySchema =
    KobbySchemaScope().apply(block).build()

@KobbyScope
class KobbySchemaScope internal constructor() {
    private val all = mutableMapOf<String, KobbyNode>()
    private val scalars = mutableMapOf<String, KobbyNode>()
    private val _query = arrayOfNulls<KobbyNode>(1)
    private val _mutation = arrayOfNulls<KobbyNode>(1)
    private val objects = mutableMapOf<String, KobbyNode>()
    private val interfaces = mutableMapOf<String, KobbyNode>()
    private val unions = mutableMapOf<String, KobbyNode>()
    private val enums = mutableMapOf<String, KobbyNode>()
    private val inputs = mutableMapOf<String, KobbyNode>()
    private val subObjectsIndex = mutableMapOf<String, MutableSet<String>>()
    private val typeOfFieldsIndex = mutableMapOf<String, MutableSet<KobbyField>>()

    val schema = KobbySchema(
        all,
        scalars,
        _query,
        _mutation,
        objects,
        interfaces,
        unions,
        enums,
        inputs,
        subObjectsIndex,
        typeOfFieldsIndex
    )

    fun addNode(
        name: String,
        kind: KobbyNodeKind,
        block: KobbyNodeScope.() -> Unit
    ) = KobbyNodeScope(schema, name, kind).apply(block).build().also { node ->
        all[node.name] = node
        when (node.kind) {
            SCALAR -> scalars[node.name] = node
            QUERY -> _query[0] = node
            MUTATION -> _mutation[0] = node
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

        node.fields { field ->
            typeOfFieldsIndex.append(field.type.nodeName, field)
        }
    }

    fun addScalar(name: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, SCALAR, block)

    fun addObject(name: String, block: KobbyNodeScope.() -> Unit) = when (name) {
        "Query" -> addNode(name, QUERY, block)
        "Mutation" -> addNode(name, MUTATION, block)
        else -> addNode(name, OBJECT, block)
    }

    fun addInterface(name: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, INTERFACE, block)

    fun addUnion(name: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, UNION, block)

    fun addEnum(name: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, ENUM, block)

    fun addInput(name: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, INPUT, block)

    fun build(): KobbySchema = schema
}