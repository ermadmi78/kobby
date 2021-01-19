package io.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbySchema(
    val all: Map<String, KobbyNode>,
    val scalars: Map<String, KobbyNode>,
    val query: Lazy<KobbyNode>,
    val mutation: Lazy<KobbyNode>,
    val objects: Map<String, KobbyNode>,
    val interfaces: Map<String, KobbyNode>,
    val unions: Map<String, KobbyNode>,
    val enums: Map<String, KobbyNode>,
    val inputs: Map<String, KobbyNode>
)