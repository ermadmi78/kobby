package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.generator.kotlin.*
import io.github.ermadmi78.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.github.ermadmi78.kobby.model.Decoration
import io.github.ermadmi78.kobby.model.KobbyDirective
import io.github.ermadmi78.kobby.model.PluginUtils.contextName
import io.github.ermadmi78.kobby.model.PluginUtils.extractCommonPrefix
import io.github.ermadmi78.kobby.model.PluginUtils.forEachPackage
import io.github.ermadmi78.kobby.model.PluginUtils.pathIterator
import io.github.ermadmi78.kobby.model.PluginUtils.removePrefixOrEmpty
import io.github.ermadmi78.kobby.model.PluginUtils.toPackageName
import io.github.ermadmi78.kobby.model._capitalize
import io.github.ermadmi78.kobby.model.parseSchema
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.FileReader

/**
 * Generate Kotlin DSL
 *
 * To print the plugin configuration, enable debug level logging:
 * ```
 * mvn -X clean compile
 * ```
 *
 * Created on 17.07.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Mojo(
    name = "generate-kotlin",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyCollection = ResolutionScope.COMPILE
)
@Suppress("unused")
class GenerateKotlinMojo : AbstractMojo() {
    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter
    private var schema: SchemaConfig = SchemaConfig()

    @Parameter
    private var kotlin: KotlinConfig = KotlinConfig()

    override fun execute() {
        if (log.isDebugEnabled) {
            log.debug("[Kobby] Plugin Configuration:\n$this")
        }

        if (!kotlin.enabled) {
            log.warn("Kobby: Kotlin DSL generation is disabled")
            return
        }

        schema.files = mutableSetOf<File>().let { schemaFiles ->
            if (schema.files.isEmpty()) {
                File(project.basedir, schema.scan.dir).scanGraphqls(schema.scan).forEach {
                    schemaFiles += it
                }
            } else {
                schema.files.asSequence().map { it.absoluteFile }.forEach { configFile ->
                    if (configFile.isFile) {
                        schemaFiles += configFile
                    } else if (configFile.isDirectory) {
                        configFile.scanGraphqls(schema.scan).forEach {
                            schemaFiles += it
                        }
                    }
                }
            }

            schemaFiles.toList()
        }

        if (schema.files.isEmpty()) {
            "GraphQL schema files not found".throwIt()
        }

        val dependencies: Set<String> = mutableSetOf<String>().also { set ->
            project.dependencies?.forEach {
                set += "${it.groupId}:${it.artifactId}"
            }
        }

        val scalars: Map<String, KotlinType> = PREDEFINED_SCALARS + kotlin.scalars.mapValues {
            it.value.toKotlinType()
        }

        if (kotlin.outputDirectory == null) {
            kotlin.outputDirectory = File(project.build.directory, "generated-sources/kobby-kotlin").absoluteFile
        }
        val targetDirectory = kotlin.outputDirectory!!
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) {
            "Failed to create directory for generated sources: $targetDirectory".throwIt()
        }
        project.addCompileSourceRoot(targetDirectory.path)

        val context = kotlin.context
        val dto = kotlin.dto
        val entity = kotlin.entity
        val impl = kotlin.impl
        val adapter = kotlin.adapter
        val ktor = kotlin.adapter.ktor

        dto.serialization.apply {
            if (enabled == null) {
                enabled = "org.jetbrains.kotlinx:kotlinx-serialization-json" in dependencies
            }
        }
        dto.jackson.apply {
            if (enabled == null) {
                enabled = "com.fasterxml.jackson.core:jackson-annotations" in dependencies
            }
        }
        ktor.apply {
            if (simpleEnabled == null || compositeEnabled == null) {
                val ktorClientCioEnabled = "io.ktor:ktor-client-cio-jvm" in dependencies
                if (simpleEnabled == null) {
                    simpleEnabled = ktorClientCioEnabled
                }
                if (compositeEnabled == null) {
                    compositeEnabled = ktorClientCioEnabled
                }
            }
        }

        log.info("[Kobby] Kotlin DSL generating...")

        val directiveLayout = mapOf(
            KobbyDirective.PRIMARY_KEY to schema.directive.primaryKey,
            KobbyDirective.REQUIRED to schema.directive.required,
            KobbyDirective.DEFAULT to schema.directive.default,
            KobbyDirective.SELECTION to schema.directive.selection
        )

        val contextName = (context.name ?: schema.files.singleOrNull()?.contextName)
            ?.filter { it.isJavaIdentifierPart() }
            ?.takeIf { it.firstOrNull()?.isJavaIdentifierStart() ?: false }
            ?: "graphql"
        val capitalizedContextName = contextName._capitalize()

        val rootPackage: List<String> = mutableListOf<String>().also { list ->
            if (kotlin.relativePackage) {
                schema.files
                    .map { it.parent.pathIterator() }
                    .extractCommonPrefix()
                    .removePrefixOrEmpty(File(project.basedir, schema.scan.dir).absoluteFile.path.pathIterator())
                    .forEach {
                        list += it
                    }
            }
            kotlin.packageName?.forEachPackage { list += it }
        }

        val contextPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            context.packageName?.forEachPackage { list += it }
        }

        val dtoPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            dto.packageName?.forEachPackage { list += it }
        }

        val dtoGraphQLPackage: List<String> = mutableListOf<String>().also { list ->
            list += dtoPackage
            dto.graphQL.packageName?.forEachPackage { list += it }
        }

        val entityPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            entity.packageName?.forEachPackage { list += it }
        }

        val implPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            impl.packageName?.forEachPackage { list += it }
        }

        val adapterKtorPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            ktor.packageName?.forEachPackage { list += it }
        }

        val layout = KotlinLayout(
            scalars,
            KotlinContextLayout(
                contextPackage.toPackageName(),
                contextName,
                Decoration(context.prefix ?: capitalizedContextName, context.postfix),
                context.query,
                context.mutation,
                context.subscription,
                context.commitEnabled
            ),
            KotlinDtoLayout(
                dtoPackage.toPackageName(),
                Decoration(dto.prefix, dto.postfix),
                Decoration(dto.enumPrefix, dto.enumPostfix),
                Decoration(dto.inputPrefix, dto.inputPostfix),
                dto.applyPrimaryKeys,
                dto.maxNumberOfFieldsForImmutableDtoClass,
                dto.maxNumberOfFieldsForImmutableInputClass,
                KotlinDtoSerialization(
                    dto.serialization.enabled!!,
                    dto.serialization.classDiscriminator,
                    dto.serialization.ignoreUnknownKeys,
                    dto.serialization.encodeDefaults,
                    dto.serialization.prettyPrint
                ),
                KotlinDtoJacksonLayout(
                    dto.jackson.enabled!!,
                    dto.jackson.typeInfoUse,
                    dto.jackson.typeInfoInclude,
                    dto.jackson.typeInfoProperty,
                    dto.jackson.jsonInclude
                ),
                KotlinDtoBuilderLayout(
                    dto.builder.enabled,
                    Decoration(dto.builder.prefix, dto.builder.postfix),
                    dto.builder.toBuilderFun,
                    dto.builder.toDtoFun,
                    dto.builder.toInputFun,
                    dto.builder.copyFun
                ),
                KotlinDtoGraphQLLayout(
                    dto.graphQL.enabled,
                    dtoGraphQLPackage.toPackageName(),
                    Decoration(
                        dto.graphQL.prefix?.trim() ?: capitalizedContextName,
                        dto.graphQL.postfix
                    )
                )
            ),
            KotlinEntityLayout(
                entity.enabled,
                entityPackage.toPackageName(),
                Decoration(entity.prefix, entity.postfix),
                entity.errorsFunName,
                entity.extensionsFunName,
                entity.contextFunEnabled,
                entity.contextFunName,
                entity.withCurrentProjectionFun,
                KotlinEntityProjectionLayout(
                    Decoration(entity.projection.projectionPrefix, entity.projection.projectionPostfix),
                    entity.projection.projectionArgument,
                    Decoration(entity.projection.withPrefix, entity.projection.withPostfix),
                    Decoration(entity.projection.withoutPrefix, entity.projection.withoutPostfix),
                    entity.projection.minimizeFun,
                    Decoration(entity.projection.qualificationPrefix, entity.projection.qualificationPostfix),
                    Decoration(
                        entity.projection.qualifiedProjectionPrefix,
                        entity.projection.qualifiedProjectionPostfix
                    ),
                    Decoration(entity.projection.onPrefix, entity.projection.onPostfix),
                    entity.projection.enableNotationWithoutParentheses
                ),
                KotlinEntitySelectionLayout(
                    Decoration(entity.selection.selectionPrefix, entity.selection.selectionPostfix),
                    entity.selection.selectionArgument,
                    Decoration(entity.selection.queryPrefix, entity.selection.queryPostfix),
                    entity.selection.queryArgument
                )
            ),
            KotlinImplLayout(
                implPackage.toPackageName(),
                Decoration(impl.prefix, impl.postfix),
                impl.internal,
                Decoration(impl.innerPrefix, impl.innerPostfix)
            ),
            KotlinAdapterLayout(
                adapter.extendedApi || !adapter.throwException,
                adapter.throwException,
                KotlinAdapterKtorLayout(
                    ktor.simpleEnabled!!,
                    ktor.compositeEnabled!!,
                    adapterKtorPackage.toPackageName(),
                    Decoration(
                        ktor.prefix?.trim() ?: capitalizedContextName,
                        ktor.postfix
                    ),
                    (ktor.receiveTimeoutMillis ?: 10_000L).takeIf { it > 0L }
                )
            )
        )

        if (layout.dto.run { serialization.enabled && jackson.enabled }) {
            log.warn("[kobby] Kotlinx serialization and Jackson serialization are not supported simultaneously.")
        }

        var kobbySchema = try {
            parseSchema(directiveLayout, *schema.files.map { FileReader(it) }.toTypedArray())
        } catch (e: Exception) {
            "Schema parsing failed.".throwIt(e)
        }

        val truncateConfig: TruncateConfig? = schema.truncate
        if (truncateConfig != null) {
            kobbySchema = try {
                kobbySchema.truncate(
                    reportEnabled = truncateConfig.reportEnabled,
                    regexEnabled = truncateConfig.regexEnabled,
                    caseSensitive = truncateConfig.caseSensitive,
                    queries = truncateConfig.queries
                ) { message ->
                    log.info(message)
                }
            } catch (e: Exception) {
                "Schema truncation failed.".throwIt(e)
            }
        }

        try {
            kobbySchema.validate().forEach { warning ->
                log.warn(warning)
            }
        } catch (e: Exception) {
            "Schema validation failed.".throwIt(e)
        }

        val output = try {
            generateKotlin(kobbySchema, layout)
        } catch (e: Exception) {
            "Kotlin DSL generation failed.".throwIt(e)
        }

        output.forEach {
            it.writeTo(targetDirectory)
        }

        log.info("[Kobby] Kotlin DSL generated $targetDirectory")
    }

    override fun toString(): String {
        return "GenerateKotlinMojo(" +
                "\n  schema=$schema, " +
                "\n  kotlin=$kotlin" +
                "\n)"
    }

    private fun String.throwIt(cause: Throwable? = null): Nothing {
        val message = "[kobby] $this${if (cause == null) "" else " " + cause.message}"
        if (cause == null) {
            log.error(message)
        } else {
            log.error(message, cause)
        }

        if (cause == null) {
            throw IllegalStateException(message)
        } else {
            throw IllegalStateException(message, cause)
        }
    }
}