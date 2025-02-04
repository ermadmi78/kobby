package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.KobbySchema
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Created on 04.03.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbySchemaQueryScopeTest {
    private fun KobbySchema.targetTypes(
        query: KobbySchemaQueryScope.() -> Unit
    ): Set<String> = KobbySchemaQueryScope(this, regexEnabled = false, caseSensitive = true)
        .apply(query)
        ._buildMap()
        .keys.asSequence()
        .map { it.name }
        .toSet()

    private fun Set<String>.shouldContains(vararg expectations: String) {
        assertEquals(expectations.toSet(), this, "Query target types")
    }

    @Test
    fun testForType() = Cinema.schema.targetTypes {
        forType("Film") { }
    }.shouldContains("Film")

    @Test
    fun testForListOfTypes() = Cinema.schema.targetTypes {
        forType("Film|Actor|Country") { }
    }.shouldContains("Film", "Actor", "Country")

    @Test
    fun testForPattern() = Cinema.schema.targetTypes {
        forType("*Inp?t") { }
    }.shouldContains("FilmInput", "ActorInput", "TagInput")

    @Test
    fun testForComplexPattern() = Cinema.schema.targetTypes {
        forType(" Co?ntry | *Inp?t ") { }
    }.shouldContains("Country", "FilmInput", "ActorInput", "TagInput")

    @Test
    fun testForNothing() = Cinema.schema.targetTypes {
        forType(" nGJVhjb<BSAWVD ") { }
    }.shouldContains()

    @Test
    fun testForQuery() = Cinema.schema.targetTypes {
        forQuery { }
    }.shouldContains("Query")

    @Test
    fun testForMutation() = Cinema.schema.targetTypes {
        forMutation { }
    }.shouldContains("Mutation")

    @Test
    fun testForSubscription() = Cinema.schema.targetTypes {
        forSubscription { }
    }.shouldContains("Subscription")

    @Test
    fun testForRoot() = Cinema.schema.targetTypes {
        forRoot { }
    }.shouldContains(
        "Query",
        "Mutation",
        "Subscription"
    )

    @Test
    fun testForAny() = Cinema.schema.targetTypes {
        forAny { }
    }.shouldContains(
        "Query",
        "Mutation",
        "Subscription",
        "Entity",
        "Taggable",
        "Country",
        "Film",
        "Genre",
        "FilmInput",
        "Actor",
        "Gender",
        "ActorInput",
        "Tag",
        "TagInput",
        "Native"
    )

    @Test
    fun testForAnyObject() = Cinema.schema.targetTypes {
        forAnyObject { }
    }.shouldContains(
        "Query",
        "Mutation",
        "Subscription",
        "Country",
        "Film",
        "Actor",
        "Tag"
    )

    @Test
    fun testForInterface() = Cinema.schema.targetTypes {
        forAnyInterface { }
    }.shouldContains(
        "Entity",
        "Taggable"
    )

    @Test
    fun testForAnyUnion() = Cinema.schema.targetTypes {
        forAnyUnion { }
    }.shouldContains(
        "Native"
    )

    @Test
    fun testForAnyEnum() = Cinema.schema.targetTypes {
        forAnyEnum { }
    }.shouldContains(
        "Genre",
        "Gender"
    )

    @Test
    fun testForAnyInput() = Cinema.schema.targetTypes {
        forAnyInput { }
    }.shouldContains(
        "FilmInput",
        "ActorInput",
        "TagInput"
    )

    @Test
    fun testForAliasInPattern() = Cinema.schema.targetTypes {
        forType(" Co?ntry | ${KobbyTypeAlias.QUERY} | *Inp?t | ${KobbyTypeAlias.ANY_ENUM} ") { }
    }.shouldContains(
        "Country",
        "Query",
        "FilmInput",
        "ActorInput",
        "TagInput",
        "Genre",
        "Gender"
    )
}