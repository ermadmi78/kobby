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
    val inputs: Map<String, KobbyNode>
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

    private val schema = KobbySchema(
        all,
        scalars,
        _query,
        _mutation,
        objects,
        interfaces,
        unions,
        enums,
        inputs
    )

    fun addNode(
        name: String,
        nativeName: String,
        kind: KobbyNodeKind,
        block: KobbyNodeScope.() -> Unit
    ) = KobbyNodeScope(schema, name, nativeName, kind).apply(block).build().also {
        all[it.name] = it
        when (it.kind) {
            SCALAR -> scalars[it.name] = it
            QUERY -> _query[0] = it
            MUTATION -> _mutation[0] = it
            OBJECT -> objects[it.name] = it
            INTERFACE -> interfaces[it.name] = it
            UNION -> unions[it.name] = it
            ENUM -> enums[it.name] = it
            INPUT -> inputs[it.name] = it
        }
    }

    fun addScalar(name: String, nativeName: String, block: KobbyNodeScope.() -> Unit) =
        addNode(name, nativeName, SCALAR, block)

    fun addObject(name: String, nativeName: String, block: KobbyNodeScope.() -> Unit) = when (nativeName) {
        "Query" -> addNode(name, nativeName, QUERY, block)
        "Mutation" -> addNode(name, nativeName, MUTATION, block)
        else -> addNode(name, nativeName, OBJECT, block)
    }

    fun build(): KobbySchema = schema
}