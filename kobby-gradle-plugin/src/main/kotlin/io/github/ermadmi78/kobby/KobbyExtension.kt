package io.github.ermadmi78.kobby

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@DslMarker
annotation class Kobby

/**
 * Kobby Kotlin Gradle Plugin project extension
 */
fun Project.kobby(configure: KobbyExtension.() -> Unit) =
    extensions.configure(KobbyExtension::class.java, configure)

/**
 * Created on 20.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Kobby
open class KobbyExtension {
    internal val schemaExtension = lazy { KobbySchemaExtension() }
    internal val kotlinExtension = lazy { KobbyKotlinExtension() }

    /** DSL schema configuration */
    fun schema(action: Action<KobbySchemaExtension>) {
        action.execute(schemaExtension.value)
    }

    /** Kotlin DSL generator configuration */
    fun kotlin(action: Action<KobbyKotlinExtension>) {
        action.execute(kotlinExtension.value)
    }
}

@Kobby
open class KobbySchemaExtension {
    var location: FileCollection? = null
    internal var directiveExtension = lazy { KobbySchemaDirectiveExtension() }

    fun directive(action: Action<KobbySchemaDirectiveExtension>) {
        action.execute(directiveExtension.value)
    }

    override fun toString(): String {
        return "KobbySchemaExtension(location=$location, directiveExtension=$directiveExtension)"
    }
}

@Kobby
open class KobbySchemaDirectiveExtension {
    var primaryKey: String? = null
    var required: String? = null
    var default: String? = null
    var selection: String? = null
    var resolve: String? = null

    override fun toString(): String {
        return "KobbySchemaDirectiveExtension(" +
                "primaryKey=$primaryKey, " +
                "required=$required, " +
                "default=$default, " +
                "selection=$selection, " +
                "resolve=$resolve" +
                ")"
    }
}

