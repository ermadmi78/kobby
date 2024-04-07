package io.github.ermadmi78.kobby.model

import org.junit.jupiter.api.Test
import java.io.InputStreamReader
import kotlin.test.assertEquals

/**
 * Created on 30.08.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class DirectiveValidationTest {
    @Test
    fun testDirectiveRestrictions() {
        "default_arguments_and_return_type".shouldViolate(
            "Restriction violated [Query.first]: The [@primaryKey] directive can only be applied to a field with no arguments.",
            "Restriction violated [Query.first]: The [@primaryKey] directive can only be applied to a field that returns a scalar or enum type.",
            "Restriction violated [Query.second]: The [@required] directive can only be applied to a field with no arguments.",
            "Restriction violated [Query.second]: The [@required] directive can only be applied to a field that returns a scalar or enum type.",
            "Restriction violated [Query.third]: The [@default] directive can only be applied to a field with no arguments.",
            "Restriction violated [Query.third]: The [@default] directive can only be applied to a field that returns a scalar or enum type."
        )

        "default_cannot_override".shouldViolate(
            "Restriction violated [Country.id]: The [@primaryKey] directive cannot be applied to overridden fields. Please, apply [@primaryKey] directive to [IBase.id] field.",
            "Restriction violated [Country.name]: The [@required] directive cannot be applied to overridden fields. Please, apply [@required] directive to [IBase.name] field.",
            "Restriction violated [Country.description]: The [@default] directive cannot be applied to overridden fields. Please, apply [@default] directive to [IBase.description] field."
        )

        "default_can_override".shouldViolate()
        "default_override".shouldViolate()
        "default_enum".shouldViolate()

        "default_mix".shouldViolate(
            "Restriction violated [Query.first]: The field is marked with several directives at once - @default, @required, @primaryKey, the behavior of the Kobby Plugin is undefined!",
            "Restriction violated [Query.second]: The field is marked with several directives at once - @default, @required, @primaryKey, the behavior of the Kobby Plugin is undefined!",
            "Restriction violated [Query.third]: The field is marked with several directives at once - @default, @required, @primaryKey, the behavior of the Kobby Plugin is undefined!",
            "Restriction violated [Query.fourth]: The field is marked with several directives at once - @default, @required, @primaryKey, the behavior of the Kobby Plugin is undefined!"
        )

        "selection_no_optional".shouldViolate(
            "Restriction violated [Query.first]: The @selection directive can only be applied to a field that contains optional arguments - nullable arguments or arguments with default value.",
            "Restriction violated [Query.second]: The @selection directive can only be applied to a field that contains optional arguments - nullable arguments or arguments with default value.",
            "Restriction violated [Query.fourth]: The @selection directive can only be applied to a field that contains optional arguments - nullable arguments or arguments with default value."
        )

        "selection_optional".shouldViolate()

        "selection_cannot_override".shouldViolate(
            "Restriction violated [Country.base]: The [@selection] directive cannot be applied to overridden fields. Please, apply [@selection] directive to [Base.base] field."
        )
        "selection_can_override".shouldViolate()
        "selection_override".shouldViolate()
    }

    private fun String.shouldViolate(vararg warnings: String) {
        val schema = parseSchema(
            emptyMap(),
            InputStreamReader(this@DirectiveValidationTest.javaClass.getResourceAsStream("$this.graphqls.txt")!!)
        )

        assertEquals(listOf(*warnings), schema.validate())
    }
}