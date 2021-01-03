package io.kobby

import org.gradle.api.Action
import org.gradle.api.Project
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

    @Volatile
    private var schemaConfigured: Boolean = false
    private val schemaExtensionLazy: KobbySchemaExtension by lazy {
        schemaConfigured = true
        KobbySchemaExtension()
    }

    internal val schemaExtension: KobbySchemaExtension?
        get() = if (schemaConfigured) schemaExtensionLazy else null

    /** Kotlin DSL schema configuration */
    fun schema(action: Action<KobbySchemaExtension>) {
        action.execute(schemaExtensionLazy)
    }

    // *****************************************************************************************************************
    //                                       Kotlin
    // *****************************************************************************************************************

    @Volatile
    private var kotlinConfigured: Boolean = false
    private val kotlinExtensionLazy: KobbyKotlinExtension by lazy {
        kotlinConfigured = true
        KobbyKotlinExtension()
    }

    internal val kotlinExtension: KobbyKotlinExtension?
        get() = if (kotlinConfigured) kotlinExtensionLazy else null

    /** Kotlin DSL generator configuration */
    fun kotlin(action: Action<KobbyKotlinExtension>) {
        action.execute(kotlinExtensionLazy)
    }
}

// *********************************************************************************************************************
//                                           Extensions
// *********************************************************************************************************************

@Kobby
open class KobbySchemaExtension {
    var local: File? = null

    override fun toString(): String =
        "KobbySchemaExtension(local=$local)"
}

