package io.github.ermadmi78.kobby.generator.kotlin

import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.ANY
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.MAP
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.STRING
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.KobbyInvalidSchemaException
import io.github.ermadmi78.kobby.model.parseSchema
import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Created on 28.08.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class SchemaValidationTest {
    private val layout = KotlinLayout(
        KotlinTypes.PREDEFINED_SCALARS + mapOf(
            "Date" to KotlinType("java.time", "LocalDate"),
            "JSON" to MAP.parameterize(STRING, ANY.nullable())
        ),
        KotlinContextLayout(
            "kobby",
            "kobby",
            Decoration("Kobby", null),
            "query",
            "mutation",
            "subscription",
            false
        ),
        KotlinDtoLayout(
            "kobby.dto",
            Decoration(null, "Dto"),
            Decoration(null, null),
            Decoration(null, null),
            true,
            245,
            245,
            KotlinDtoSerialization(
                true,
                "serialization",
                true,
                false,
                false
            ),
            KotlinDtoJacksonLayout(
                true,
                "NAME",
                "PROPERTY",
                "__typename",
                "NON_ABSENT"
            ),
            KotlinDtoBuilderLayout(
                true,
                Decoration(null, "Builder"),
                "toBuilder",
                "toDto",
                "toInput",
                "copy"
            ),
            KotlinDtoGraphQLLayout(
                true,
                "kobby.dto.graphql",
                Decoration("GraphQL", null)
            )
        ),
        KotlinEntityLayout(
            true,
            "kobby.entity",
            Decoration(null, null),
            "__errors",
            "__extensions",
            true,
            "__context",
            "__withCurrentProjection",
            KotlinEntityProjectionLayout(
                Decoration(null, "Projection"),
                "__projection",
                Decoration(null, null),
                Decoration("__without", null),
                "__minimize",
                Decoration(null, "Qualification"),
                Decoration(null, "QualifiedProjection"),
                Decoration("__on", null),
                false
            ),
            KotlinEntitySelectionLayout(
                Decoration(null, "Selection"),
                "__selection",
                Decoration(null, "Query"),
                "__query"
            )
        ),
        KotlinImplLayout(
            "kobby.entity.impl",
            Decoration(null, "Impl"),
            true,
            Decoration("__inner", null)
        ),
        KotlinAdapterLayout(
            false,
            true,
            KotlinAdapterKtorLayout(
                true,
                true,
                "kobby.adapter.ktor",
                Decoration("Kobby", "Adapter"),
                null
            )
        )
    )

    //@Test
    fun temp() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("kobby.graphqls")!!)
        )

        schema.validate().forEach {
            println(it)
        }

        val files = generateKotlin(schema, layout)

        files.forEach {
            println()
            it.writeTo(System.out)
            println("---------")
        }
    }

    @Test
    fun testUnknownScalar() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_scalar.graphqls.txt")!!)
        )

        assertTrue(schema.validate().isEmpty())

        val expected = "Kotlin data type for scalar 'DummyScalar' not found. " +
                "Please, configure it by means of 'kobby' extension. https://github.com/ermadmi78/kobby"
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: IllegalStateException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownType() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_type.graphqls.txt")!!)
        )

        assertTrue(schema.validate().isEmpty())

        val expected = "Unknown type \"DummyType\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownArgType() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_arg_type.graphqls.txt")!!)
        )

        assertTrue(schema.validate().isEmpty())

        val expected = "Unknown type \"DummyArg\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownParent() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_parent.graphqls.txt")!!)
        )

        assertTrue(schema.validate().isEmpty())

        val expected = "Unknown type \"DummyParent\""
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun testUnknownParentWithDefault() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("unknown_parent_with_default.graphqls.txt")!!)
        )

        val expected = "Unknown type \"DummyParentWithDefault\""
        try {
            schema.validate()
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }

    @Test
    fun textInheritedFieldAbsent() {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this.javaClass.getResourceAsStream("inherited_field_absent.graphqls.txt")!!)
        )

        assertTrue(schema.validate().isEmpty())

        val expected = "The object type 'Country' does not have a field 'bug' required via interface 'Bug'"
        try {
            generateKotlin(schema, layout)
            fail("Must throw: $expected")
        } catch (e: KobbyInvalidSchemaException) {
            assertEquals(expected, e.message)
        }
    }
}