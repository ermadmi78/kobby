package io.github.ermadmi78.kobby.server.controller

import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType

val graphqlIDType = GraphQLScalarType.newScalar()
    .name("ID")
    .description("A type representing a Long")
    .coercing(IDCoercing as Coercing<*, *>)
    .build()

object IDCoercing : Coercing<Long, String> {
    override fun parseValue(input: Any): Long = serialize(input).toLong()
    override fun parseLiteral(input: Any): Long? = (input as? StringValue)?.value?.toLong()
    override fun serialize(dataFetcherResult: Any): String = dataFetcherResult.toString()
}

