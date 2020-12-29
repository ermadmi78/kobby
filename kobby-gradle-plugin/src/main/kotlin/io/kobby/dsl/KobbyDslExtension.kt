package io.kobby.dsl

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import java.io.File

@DslMarker
@MustBeDocumented
annotation class KobbyDsl

/**
 * Kobby Kotlin Gradle Plugin project extension
 */
fun Project.kobbyDSL(configure: KobbyDslExtension.() -> Unit) =
    extensions.configure(KobbyDslExtension::class.java, configure)

/**
 * Created on 20.12.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@KobbyDsl
open class KobbyDslExtension {
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

@KobbyDsl
open class KobbySchemaExtension {
    var local: File? = null

    override fun toString(): String =
        "KobbySchemaExtension(local=$local)"
}

data class KobbyTypeName(
    /** A fully-qualified package name. For example: kotlin.collections */
    val packageName: String,

    /** A fully-qualified class name. For example: Map.Entry */
    val className: String,

    /** Is type nullable */
    val allowNull: Boolean = false,

    /** List of generics */
    val generics: List<KobbyTypeName> = listOf()
) {
    fun nullable(): KobbyTypeName =
        if (allowNull) this else KobbyTypeName(packageName, className, true, generics)

    fun parameterize(vararg arguments: KobbyTypeName): KobbyTypeName =
        if (arguments.isEmpty()) this else KobbyTypeName(packageName, className, allowNull, generics + arguments)

    fun nested(nestedClassName: String): KobbyTypeName =
        KobbyTypeName(packageName, "$className.$nestedClassName", allowNull, generics)

    override fun toString(): String {
        return "$packageName.$className" +
                (if (generics.isNotEmpty()) "<${generics.joinToString { it.toString() }}>" else "") +
                (if (allowNull) "?" else "")
    }
}