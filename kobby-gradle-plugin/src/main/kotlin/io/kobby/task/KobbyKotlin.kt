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
import java.io.File
import java.io.FileReader

const val KOBBY_KOTLIN_TASK_NAME = "kobbyKotlin"

/**
 * Created on 02.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Suppress("UnstableApiUsage")
open class KobbyKotlin : DefaultTask() {
    /**
     * Path to GraphQL schema file that will be used to generate client code.
     *
     * **Required Property**: [schemaFileName] or [schemaFile] has to be provided.
     * **Command line property is**: `schemaFileName`.
     */
    @Input
    @Optional
    @Option(
        option = "schemaFileName",
        description = "path to GraphQL schema file that will be used to generate the client code"
    )
    val schemaFileName: Property<String> = project.objects.property(String::class.java)

    /**
     * GraphQL schema file that will be used to generate client code.
     *
     * **Required Property**: [schemaFileName] or [schemaFile] has to be provided.
     */
    @InputFile
    @Optional
    val schemaFile: RegularFileProperty = project.objects.fileProperty()

    @Input
    @Optional
    val scalars: MapProperty<String, KotlinType> =
        project.objects.mapProperty(String::class.java, KotlinType::class.java)

    @Input
    @Option(
        option = "dtoPackageName",
        description = "target package name to use for generated DTO classes"
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
        option = "dtoJacksonized",
        description = "add Jackson annotations for generated DTO classes (default true)"
    )
    val dtoJacksonized: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "dtoBuilders",
        description = "generate builders for generated DTO classes (default true)"
    )
    val dtoBuilders: Property<Boolean> = project.objects.property(Boolean::class.java)

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
        description = "target package name to use for generated API classes"
    )
    val apiPackageName: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "implPackageName",
        description = "target package name to use for generated implementation classes"
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

        scalars.convention(PREDEFINED_SCALARS)
        dtoPostfix.convention("Dto")
        dtoJacksonized.convention(true)
        dtoBuilders.convention(true)
        apiEnabled.convention(true)
        implPostfix.convention("Impl")
    }

    @TaskAction
    fun generateKotlinDslClientAction() {
        val graphQLSchema = when {
            schemaFileName.isPresent -> File(schemaFileName.get())
            schemaFile.isPresent -> schemaFile.get().asFile
            else -> throw RuntimeException("schema not available")
        }
        if (!graphQLSchema.isFile) {
            throw RuntimeException("specified schema file does not exist")
        }

        val dslApiEnabled = apiEnabled.get()
        val layout = KotlinGeneratorLayout(
            scalars.get(),
            KotlinDtoLayout(
                dtoPackageName.get(),
                dtoPrefix.orNull,
                dtoPostfix.orNull,
                jacksonized = dtoJacksonized.get(),
                builders = dtoBuilders.get()
            ),
            KotlinApiLayout(
                dslApiEnabled,
                if (dslApiEnabled) apiPackageName.get() else ""
            ),
            KotlinImplLayout(
                if (dslApiEnabled) implPackageName.get() else "",
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
}