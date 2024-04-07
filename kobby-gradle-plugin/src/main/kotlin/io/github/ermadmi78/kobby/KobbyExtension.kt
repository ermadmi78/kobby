package io.github.ermadmi78.kobby

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection

@DslMarker
annotation class Kobby

/**
 * Kobby Plugin Configuration
 */
fun Project.kobby(configure: KobbyExtension.() -> Unit) =
    extensions.configure(KobbyExtension::class.java, configure)

/**
 * Kobby Plugin Configuration
 */
@Kobby
open class KobbyExtension {
    internal val schemaExtension = lazy { KobbySchemaExtension() }
    internal val kotlinExtension = lazy { KobbyKotlinExtension() }

    /** Schema location and parsing rules configuration */
    fun schema(action: Action<KobbySchemaExtension>) {
        action.execute(schemaExtension.value)
    }

    /** Configuration of Kotlin DSL generation */
    fun kotlin(action: Action<KobbyKotlinExtension>) {
        action.execute(kotlinExtension.value)
    }
}

/**
 * Schema location and parsing rules configuration
 */
@Kobby
open class KobbySchemaExtension {
    /**
     * GraphQL schema files to generate Kobby DSL.
     *
     * By default, all "`**`/`*`.graphqls" files in "src/main/resources"
     */
    var files: FileCollection? = null

    internal var scanExtension = lazy { KobbySchemaScanExtension() }
    internal var directiveExtension = lazy { KobbySchemaDirectiveExtension() }

    /** Configuration of schema files location scanning */
    fun scan(action: Action<KobbySchemaScanExtension>) {
        action.execute(scanExtension.value)
    }

    /** Configuration of Kobby GraphQL directives parsing */
    fun directive(action: Action<KobbySchemaDirectiveExtension>) {
        action.execute(directiveExtension.value)
    }
}

/**
 * Configuration of schema files location scanning
 */
@Kobby
open class KobbySchemaScanExtension {
    /**
     * Root directory to scan schema files
     *
     * Default: "src/main/resources"
     */
    var dir: String? = null

    /**
     * ANT style include patterns to scan schema files
     *
     * Default: listOf("`**`/`*`.graphqls")
     */
    var includes: Iterable<String>? = null

    /** ANT style exclude patterns to scan schema files */
    var excludes: Iterable<String>? = null
}

/**
 * Configuration of Kobby GraphQL directives parsing
 */
@Kobby
open class KobbySchemaDirectiveExtension {
    /**
     * Name of "primaryKey" directive
     *
     * Default: "primaryKey"
     */
    var primaryKey: String? = null

    /**
     * Name of "required" directive
     *
     * Default: "required"
     */
    var required: String? = null

    /**
     * Name of "default" directive
     *
     * Default: "default"
     */
    var default: String? = null

    /**
     * Name of "selection" directive
     *
     * Default: "selection"
     */
    var selection: String? = null
}

