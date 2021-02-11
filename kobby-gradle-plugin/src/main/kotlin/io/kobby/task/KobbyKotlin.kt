package io.kobby.task

import io.kobby.generator.kotlin.*
import io.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import io.kobby.model.Decoration
import io.kobby.model.KobbyDirective
import io.kobby.model.parseSchema
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.options.Option
import java.io.FileReader

/**
 * Created on 02.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Suppress("UnstableApiUsage")
open class KobbyKotlin : DefaultTask() {
    companion object {
        const val TASK_NAME = "kobbyKotlin"
    }

    /**
     * GraphQL schema file that will be used to generate client code.
     */
    @InputFile
    val schemaFile: RegularFileProperty = project.objects.fileProperty()

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveRequired",
        description = "name of directive \"required\" (default \"required\")"
    )
    val schemaDirectiveRequired: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveDefault",
        description = "name of directive \"default\" (default \"default\")"
    )
    val schemaDirectiveDefault: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveSelection",
        description = "name of directive \"selection\" (default \"selection\")"
    )
    val schemaDirectiveSelection: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    val scalars: MapProperty<String, KotlinType> =
        project.objects.mapProperty(String::class.java, KotlinType::class.java)


    @Input
    @Optional
    @Option(
        option = "relativePackage",
        description = "generate root package name relative to schema package name (default true)"
    )
    val relativePackage: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "rootPackageName",
        description = "root package name relative to schema package name (if relativePackage option is true) " +
                "for generated classes (default \"kobby.kotlin\")"
    )
    val rootPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPackageName",
        description = "package name relative to root package name for generated context classes (default null)"
    )
    val contextPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextName",
        description = "name of context (default \"<GraphQL schema name>\")"
    )
    val contextName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPrefix",
        description = "prefix for generated context classes (default \"<Context name>\")"
    )
    val contextPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextPostfix",
        description = "postfix for generated context classes (default null)"
    )
    val contextPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextQuery",
        description = "name of context query function (default \"query\")"
    )
    val contextQuery: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "contextQuery",
        description = "name of context mutation function (default \"mutation\")"
    )
    val contextMutation: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPackageName",
        description = "package name relative to root package name for generated DTO classes (default \"dto\")"
    )
    val dtoPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPrefix",
        description = "prefix for generated DTO classes (default null)"
    )
    val dtoPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoPostfix",
        description = "postfix for generated DTO classes (default \"Dto\")"
    )
    val dtoPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoJacksonEnabled",
        description = "add Jackson annotations for generated DTO classes (default true)"
    )
    val dtoJacksonEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderEnabled",
        description = "generate DTO builders is enabled (default true)"
    )
    val dtoBuilderEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPrefix",
        description = "prefix for generated DTO Builder classes (default null)"
    )
    val dtoBuilderPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilderPostfix",
        description = "postfix for generated DTO Builder classes (default \"Dto\")"
    )
    val dtoBuilderPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLEnabled",
        description = "generate GraphQL DTO classes (default true)"
    )
    val dtoGraphQLEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPackageName",
        description = "package name for GraphQL DTO classes relative to DTO package name (default \"graphql\")"
    )
    val dtoGraphQLPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPrefix",
        description = "prefix for generated GraphQL DTO classes (default \"<Context name>\")"
    )
    val dtoGraphQLPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoGraphQLPostfix",
        description = "postfix for generated GraphQL DTO classes (default null)"
    )
    val dtoGraphQLPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityEnabled",
        description = "generate Entity classes (default true)"
    )
    val entityEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPackageName",
        description = "package name relative to root package name for generated Entity classes (default \"entity\")"
    )
    val entityPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPrefix",
        description = "prefix for generated entity classes (default null)"
    )
    val entityPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityPostfix",
        description = "postfix for generated entity classes (default null)"
    )
    val entityPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithCurrentProjectionFun",
        description = "name of entity 'withCurrentProjection' function (default \"__withCurrentProjection\")"
    )
    val entityWithCurrentProjectionFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPrefix",
        description = "prefix for generated projection classes (default null)"
    )
    val entityProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionPostfix",
        description = "postfix for generated projection classes (default \"Projection\")"
    )
    val entityProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityProjectionArgument",
        description = "name of projection lambda argument (default \"__projection\")"
    )
    val entityProjectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPrefix",
        description = "prefix of projection 'with' method (default null)"
    )
    val entityWithPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithPostfix",
        description = "postfix of projection 'with' method (default null)"
    )
    val entityWithPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPrefix",
        description = "prefix of projection 'with' method (default \"__without\")"
    )
    val entityWithoutPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityWithoutPostfix",
        description = "postfix of projection 'with' method (default null)"
    )
    val entityWithoutPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityMinimizeFun",
        description = "name of projection 'minimize' function (default \"__minimize\")"
    )
    val entityMinimizeFun: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPrefix",
        description = "prefix for generated qualification classes (default null)"
    )
    val entityQualificationPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualificationPostfix",
        description = "postfix for generated qualification classes (default \"Qualification\")"
    )
    val entityQualificationPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPrefix",
        description = "prefix for generated qualification classes (default null)"
    )
    val entityQualifiedProjectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQualifiedProjectionPostfix",
        description = "postfix for generated qualification classes (default \"QualifiedProjection\")"
    )
    val entityQualifiedProjectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPrefix",
        description = "prefix of qualification 'on' method (default \"__on\")"
    )
    val entityOnPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityOnPostfix",
        description = "postfix of qualification 'on' method (default null)"
    )
    val entityOnPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPrefix",
        description = "prefix for generated selection classes (default null)"
    )
    val entitySelectionPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionPostfix",
        description = "postfix for generated selection classes (default \"Selection\")"
    )
    val entitySelectionPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entitySelectionArgument",
        description = "name of selection lambda argument (default \"__selection\")"
    )
    val entitySelectionArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPrefix",
        description = "prefix for generated query classes (default null)"
    )
    val entityQueryPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryPostfix",
        description = "postfix for generated query classes (default \"Query\")"
    )
    val entityQueryPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "entityQueryArgument",
        description = "name of query lambda argument (default \"__query\")"
    )
    val entityQueryArgument: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPackageName",
        description = "package name relative to root package name " +
                "for generated implementation classes (default \"impl\")"
    )
    val implPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPrefix",
        description = "prefix for generated implementation classes (default null)"
    )
    val implPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPostfix",
        description = "postfix for generated implementation classes (default \"Impl\")"
    )
    val implPostfix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInternal",
        description = "make generated implementation classes internal (default true)"
    )
    val implInternal: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPrefix",
        description = "prefix for generated implementation service properties (default \"__inner\")"
    )
    val implInnerPrefix: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implInnerPostfix",
        description = "postfix for generated implementation service properties (default \"Impl\")"
    )
    val implInnerPostfix: Property<String> = project.objects.property(String::class.java)

    @OutputDirectory
    val outputDirectory: DirectoryProperty = project.objects.directoryProperty()

    init {
        group = "kobby"
        description = "Generate Kotlin DSL client by GraphQL schema"

        schemaFile.convention(project.layout.file(project.provider {
            project.fileTree("src/main/resources") {
                it.include("**/*.graphqls")
            }.filter { it.isFile }.singleFile
        }))
        schemaDirectiveRequired.convention(KobbyDirective.REQUIRED)
        schemaDirectiveDefault.convention(KobbyDirective.DEFAULT)
        schemaDirectiveSelection.convention(KobbyDirective.SELECTION)

        scalars.convention(PREDEFINED_SCALARS)

        relativePackage.convention(true)
        rootPackageName.convention("kobby.kotlin")

        contextQuery.convention("query")
        contextMutation.convention("mutation")

        dtoPackageName.convention("dto")
        dtoPostfix.convention("Dto")
        dtoJacksonEnabled.convention(project.provider {
            project.hasDependency("com.fasterxml.jackson.core", "jackson-annotations")
        })
        dtoBuilderEnabled.convention(true)
        dtoBuilderPostfix.convention("Builder")
        dtoGraphQLEnabled.convention(true)
        dtoGraphQLPackageName.convention("graphql")

        entityEnabled.convention(true)
        entityPackageName.convention("entity")
        entityWithCurrentProjectionFun.convention("__withCurrentProjection")
        entityProjectionPostfix.convention("Projection")
        entityProjectionArgument.convention("__projection")
        entityWithoutPrefix.convention("__without")
        entityMinimizeFun.convention("__minimize")
        entityQualificationPostfix.convention("Qualification")
        entityQualifiedProjectionPostfix.convention("QualifiedProjection")
        entityOnPrefix.convention("__on")
        entitySelectionPostfix.convention("Selection")
        entitySelectionArgument.convention("__selection")
        entityQueryPostfix.convention("Query")
        entityQueryArgument.convention("__query")

        implPackageName.convention("entity.impl")
        implPostfix.convention("Impl")
        implInternal.convention(true)
        implInnerPrefix.convention("__inner")

        outputDirectory.convention(project.layout.buildDirectory.dir("generated/source/kobby/main/kotlin"))
    }

    @TaskAction
    fun generateKotlinDslClientAction() {
        val graphQLSchema = schemaFile.get().asFile.absoluteFile
        if (!graphQLSchema.isFile) {
            throw RuntimeException("specified schema file does not exist")
        }

        val directiveLayout = mapOf(
            KobbyDirective.REQUIRED to schemaDirectiveRequired.get(),
            KobbyDirective.DEFAULT to schemaDirectiveDefault.get(),
            KobbyDirective.SELECTION to schemaDirectiveSelection.get()
        )

        val context = (contextName.orNull
            ?: graphQLSchema.name
                .splitToSequence('.')
                .filter { it.isNotBlank() }
                .firstOrNull()
                ?.decapitalize())
            ?.filter { it.isJavaIdentifierPart() }
            ?.takeIf { it.firstOrNull()?.isJavaIdentifierStart() ?: false }
            ?: "graphql"
        val capitalizedContext = context.capitalize()

        val rootPackage: List<String> = mutableListOf<String>().also { list ->
            if (relativePackage.get()) {
                val resourcesDir = project.file("src/main/resources").absoluteFile.path
                val schemaDir = graphQLSchema.parent
                if (schemaDir.startsWith(resourcesDir)) {
                    schemaDir.removePrefix(resourcesDir).forEachPath { list += it }
                }
            }
            rootPackageName.orNull?.forEachPackage { list += it }
        }

        val contextPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            contextPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            dtoPackageName.orNull?.forEachPackage { list += it }
        }

        val dtoGraphQLPackage: List<String> = mutableListOf<String>().also { list ->
            list += dtoPackage
            dtoGraphQLPackageName.orNull?.forEachPackage { list += it }
        }

        val entityPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            entityPackageName.orNull?.forEachPackage { list += it }
        }

        val implPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            implPackageName.orNull?.forEachPackage { list += it }
        }

        val layout = KotlinLayout(
            scalars.get(),
            KotlinContextLayout(
                contextPackage.toPackageName(),
                context,
                Decoration(capitalizedContext, null),
                contextQuery.get(),
                contextMutation.get()
            ),
            KotlinDtoLayout(
                dtoPackage.toPackageName(),
                Decoration(dtoPrefix.orNull, dtoPostfix.orNull),
                KotlinDtoJacksonLayout(dtoJacksonEnabled.get()),
                KotlinDtoBuilderLayout(
                    dtoBuilderEnabled.get(),
                    Decoration(dtoBuilderPrefix.orNull, dtoBuilderPostfix.orNull)
                ),
                KotlinDtoGraphQLLayout(
                    dtoGraphQLEnabled.get(),
                    dtoGraphQLPackage.toPackageName(),
                    Decoration(
                        dtoGraphQLPrefix.orNull?.trim() ?: capitalizedContext,
                        dtoGraphQLPostfix.orNull
                    )
                )
            ),
            KotlinEntityLayout(
                entityEnabled.get(),
                entityPackage.toPackageName(),
                Decoration(entityPrefix.orNull, entityPostfix.orNull),
                entityWithCurrentProjectionFun.get(),
                KotlinEntityProjectionLayout(
                    Decoration(entityProjectionPrefix.orNull, entityProjectionPostfix.orNull),
                    entityProjectionArgument.get(),
                    Decoration(entityWithPrefix.orNull, entityWithPostfix.orNull),
                    Decoration(entityWithoutPrefix.orNull, entityWithoutPostfix.orNull),
                    entityMinimizeFun.get(),
                    Decoration(entityQualificationPrefix.orNull, entityQualificationPostfix.orNull),
                    Decoration(entityQualifiedProjectionPrefix.orNull, entityQualifiedProjectionPostfix.orNull),
                    Decoration(entityOnPrefix.orNull, entityOnPostfix.orNull)
                ),
                KotlinEntitySelectionLayout(
                    Decoration(entitySelectionPrefix.orNull, entitySelectionPostfix.orNull),
                    entitySelectionArgument.get(),
                    Decoration(entityQueryPrefix.orNull, entityQueryPostfix.orNull),
                    entityQueryArgument.get()
                )
            ),
            KotlinImplLayout(
                implPackage.toPackageName(),
                Decoration(implPrefix.orNull, implPostfix.orNull),
                implInternal.get(),
                Decoration(implInnerPrefix.orNull, implInnerPostfix.orNull)
            )
        )

        val targetDirectory = outputDirectory.get().asFile
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) {
            throw RuntimeException("failed to generate generated source directory")
        }

        val schema = parseSchema(directiveLayout, FileReader(graphQLSchema))
        val output = generateKotlin(schema, layout)
        output.forEach {
            it.writeTo(targetDirectory)
        }
    }

    private fun Project.resolveDependencies(): Sequence<ResolvedDependency> = this.configurations.asSequence()
        .filter { it.isCanBeResolved }
        .flatMap { it.resolvedConfiguration.firstLevelModuleDependencies }
        .flatMap {
            sequence {
                yield(it)
                yieldAll(it.children)
            }
        }

    private fun Project.hasDependency(moduleGroup: String, moduleName: String): Boolean =
        this.resolveDependencies().any {
            it.moduleGroup == moduleGroup && it.moduleName == moduleName
        }

    private fun String.forEachPath(action: (String) -> Unit) =
        this.splitToSequence('/', '\\')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(action)

    private fun String.forEachPackage(action: (String) -> Unit) =
        this.splitToSequence('.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(action)

    private fun List<String>.toPackageName(): String = joinToString(".") { it }
}