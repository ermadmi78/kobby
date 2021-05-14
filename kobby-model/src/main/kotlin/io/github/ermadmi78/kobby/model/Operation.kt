package io.github.ermadmi78.kobby.model

/**
 * Created on 17.05.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
enum class Operation(
    private val operationName: String,
    val defaultNodeName: String
) {
    QUERY("query", "Query"),
    MUTATION("mutation", "Mutation"),
    SUBSCRIPTION("subscription", "Subscription");

    companion object {
        fun of(name: String?): Operation? = values().firstOrNull {
            it.operationName == name
        }
    }
}