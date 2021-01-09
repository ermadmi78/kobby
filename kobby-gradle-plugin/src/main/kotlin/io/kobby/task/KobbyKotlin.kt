package io.kobby.task

import io.kobby.generator.kotlin.*
import io.kobby.generator.kotlin.KotlinTypes.PREDEFINED_SCALARS
import org.gradle.api.DefaultTask
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
     *
     * **Required Property**: [schemaFileName] or [schemaFile] has to be provided.
     */
    @InputFile
    val schemaFile: RegularFileProperty = project.objects.fileProperty()

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
        option = "apiEnabled",
        description = "generate API classes (default true)"
    )
    val apiEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "apiPackageName",
        description = "package name relative to root package name for generated API classes (default null)"
    )
    val apiPackageName: Property<String> = project.objects.property(String::class.java)

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
        scalars.convention(PREDEFINED_SCALARS)

        relativePackage.convention(true)
        rootPackageName.convention("kobby.kotlin")

        dtoPackageName.convention("dto")
        dtoPostfix.convention("Dto")
        dtoJacksonEnabled.convention(true)
        dtoBuilderEnabled.convention(true)
        dtoBuilderPostfix.convention("Builder")

        apiEnabled.convention(true)

        implPackageName.convention("impl")
        implPostfix.convention("Impl")

        outputDirectory.convention(project.layout.buildDirectory.dir("generated/source/kobby/main/kotlin"))
    }

    @TaskAction
    fun generateKotlinDslClientAction() {
        val graphQLSchema = schemaFile.get().asFile.absoluteFile
        if (!graphQLSchema.isFile) {
            throw RuntimeException("specified schema file does not exist")
        }

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

        val dtoPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            dtoPackageName.orNull?.forEachPackage { list += it }
        }

        val apiPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            apiPackageName.orNull?.forEachPackage { list += it }
        }

        val implPackage: List<String> = mutableListOf<String>().also { list ->
            list += rootPackage
            implPackageName.orNull?.forEachPackage { list += it }
        }

        val layout = KotlinGeneratorLayout(
            scalars.get(),
            KotlinDtoLayout(
                dtoPackage.toPackageName(),
                dtoPrefix.orNull,
                dtoPostfix.orNull,
                KotlinDtoJacksonLayout(dtoJacksonEnabled.get()),
                KotlinDtoBuilderLayout(
                    dtoBuilderEnabled.get(),
                    dtoBuilderPrefix.get(),
                    dtoBuilderPostfix.get()
                )
            ),
            KotlinApiLayout(
                apiEnabled.get(),
                apiPackage.toPackageName()
            ),
            KotlinImplLayout(
                implPackage.toPackageName(),
                implPrefix.orNull,
                implPostfix.orNull
            )
        )

        val targetDirectory = outputDirectory.get().asFile
        if (!targetDirectory.isDirectory && !targetDirectory.mkdirs()) {
            throw RuntimeException("failed to generate generated source directory")
        }

        val output = generateKotlin(layout, FileReader(graphQLSchema))
        output.dtoFiles.forEach {
            it.writeTo(targetDirectory)
        }
        output.apiFiles.forEach {
            it.writeTo(targetDirectory)
        }
        output.implFiles.forEach {
            it.writeTo(targetDirectory)
        }
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