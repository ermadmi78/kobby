package io.kobby

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import java.io.File

@DslMarker
@MustBeDocumented
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
    // *****************************************************************************************************************
    //                                       Schema
    // *****************************************************************************************************************
    private var schemaConfigured: Boolean = false

    internal val schemaExtension: KobbySchemaExtension by lazy {
        schemaConfigured = true
        KobbySchemaExtension()
    }

    internal fun isSchemaConfigured(): Boolean = schemaConfigured

    /** Kotlin DSL schema configuration */
    fun schema(action: Action<KobbySchemaExtension>) {
        action.execute(schemaExtension)
    }


    var schemaSearchTree: ConfigurableFileTree? = null

    // *****************************************************************************************************************
    //                                       Kotlin
    // *****************************************************************************************************************

    private var kotlinConfigured: Boolean = false
    internal val kotlinExtension: KobbyKotlinExtension by lazy {
        kotlinConfigured = true
        KobbyKotlinExtension()
    }

    internal fun isKotlinConfigured(): Boolean = kotlinConfigured

    /** Kotlin DSL generator configuration */
    fun kotlin(action: Action<KobbyKotlinExtension>) {
        action.execute(kotlinExtension)
    }
}

@Kobby
open class KobbySchemaExtension {
    var local: File? = null

    override fun toString(): String =
        "KobbySchemaExtension(local=$local)"
}

