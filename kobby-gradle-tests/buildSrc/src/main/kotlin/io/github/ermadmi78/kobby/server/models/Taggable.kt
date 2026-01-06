package io.github.ermadmi78.kobby.server.models

import com.expediagroup.graphql.generator.annotations.GraphQLIgnore

interface Taggable : Entity {
    val tags: List<Tag>

    @GraphQLIgnore
    fun containsTag(tag: String): Boolean = tags.any { it.value != null && it.value == tag }
}
