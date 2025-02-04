package io.github.ermadmi78.kobby.task

import io.github.ermadmi78.kobby.model.KobbyDirective
import io.github.ermadmi78.kobby.model.parseSchema
import io.github.ermadmi78.kobby.model.query.KobbyTypeAlias
import io.github.ermadmi78.kobby.model.query.Path
import io.github.ermadmi78.kobby.model.query.reportQueryPropertiesStr
import io.github.ermadmi78.kobby.model.query.reportWeightStr
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File
import java.io.FileReader

/**
 * Created on 08.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
open class KobbySchemaAnalyze : DefaultTask() {
    companion object {
        const val TASK_NAME = "kobbySchemaAnalyze"
    }

    @InputFiles
    val schemaFiles: ListProperty<RegularFile> = project.objects.listProperty(RegularFile::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaScanDir",
        description = "Root directory to scan schema files (default \"src/main/resources\")"
    )
    val schemaScanDir: Property<String> = project.objects.property(String::class.java)

    /**
     * ANT style include patterns to scan schema files (default `**`/`*`.graphqls)
     */
    @Input
    @Optional
    val schemaScanIncludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * ANT style exclude patterns to scan schema files (default empty)
     */
    @Input
    @Optional
    val schemaScanExcludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectivePrimaryKey",
        description = "Name of \"primaryKey\" directive (default \"primaryKey\")"
    )
    val schemaDirectivePrimaryKey: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveRequired",
        description = "Name of \"required\" directive (default \"required\")"
    )
    val schemaDirectiveRequired: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveDefault",
        description = "Name of \"default\" directive (default \"default\")"
    )
    val schemaDirectiveDefault: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaDirectiveSelection",
        description = "Name of \"selection\" directive (default \"selection\")"
    )
    val schemaDirectiveSelection: Property<String> = project.objects.property(String::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateReportEnabled",
        description = "Print detailed schema truncation report to console (default false)"
    )
    val schemaTruncateReportEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateRegexEnabled",
        description = "true - use Regex in truncation query, false - use Kobby Pattern " +
                "(? - matches one character, * - matches zero or more characters, | - OR operator). " +
                "Default false."
    )
    val schemaTruncateRegexEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "schemaTruncateCaseSensitive",
        description = "Are patterns used in a GraphQL schema truncation query case sensitive (default true)"
    )
    val schemaTruncateCaseSensitive: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    val schemaTruncateQuery: MapProperty<String, KobbyTypeOperationQuery> =
        project.objects.mapProperty(String::class.java, KobbyTypeOperationQuery::class.java)

    //*****************************************************************************************************************

    @Input
    @Optional
    @Option(
        option = "truncatedSchema",
        description = "true - use a truncated GraphQL schema for analysis, else use original schema (default false)"
    )
    val truncatedSchema: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "depth",
        description = "GraphQL schema analysis depth. Use -1 to analyse a schema with unlimited depth. (default 1)"
    )
    val depth: Property<Int> = project.objects.property(Int::class.java)

    @Input
    @Optional
    @Option(
        option = "reportLengthLimit",
        description = "GraphQL schema analysis report length limit. Use -1 to print a report with unlimited length. " +
                "(default 10000)"
    )
    val reportLengthLimit: Property<Int> = project.objects.property(Int::class.java)

    @Input
    @Optional
    @Option(
        option = "printMinWeight",
        description = "The minimum weight of a GraphQL type that should be printed in the report (default 2)"
    )
    val printMinWeight: Property<Int> = project.objects.property(Int::class.java)

    @Input
    @Optional
    @Option(
        option = "printOverride",
        description = "Print \"override sign\" (^) in report for overridden GraphQL type fields (default false)"
    )
    val printOverride: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "printArgumentTypes",
        description = "Print GraphQL field argument types in a report (default false)"
    )
    val printArgumentTypes: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "printSuperTypes",
        description = "Print GraphQL field supertypes in report (`<-` followed by a list of supertypes). Default false"
    )
    val printSuperTypes: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "printSubTypes",
        description = "Print GraphQL field subtypes in report (`->` followed by a list of subtypes). Default false"
    )
    val printSubTypes: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "regexEnabled",
        description = "true - use Regex in analyze query, false - use Kobby Pattern " +
                "(? - matches one character, * - matches zero or more characters, | - OR operator). " +
                "Default false."
    )
    val regexEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    @Option(
        option = "caseSensitive",
        description = "Are patterns used in a GraphQL schema analyze query case sensitive (default true)"
    )
    val caseSensitive: Property<Boolean> = project.objects.property(Boolean::class.java)

    @Input
    @Optional
    val query: MapProperty<String, KobbyTypeOperationQuery> =
        project.objects.mapProperty(String::class.java, KobbyTypeOperationQuery::class.java)

    init {
        group = "kobby"
        description = "Print GraphQL schema dependency tree"

        schemaFiles.convention(project.provider<Iterable<RegularFile>> {
            project.fileTree(schemaScanDir.get()) {
                it.include(schemaScanIncludes.get())
                it.exclude(schemaScanExcludes.get())
            }.filter { it.isFile }.files.map {
                project.layout.file(project.provider { it }).get()
            }
        })

        schemaScanDir.convention("src/main/resources")
        schemaScanIncludes.convention(listOf("**/*.graphqls"))
        schemaScanExcludes.convention(listOf())

        schemaDirectivePrimaryKey.convention(KobbyDirective.PRIMARY_KEY)
        schemaDirectiveRequired.convention(KobbyDirective.REQUIRED)
        schemaDirectiveDefault.convention(KobbyDirective.DEFAULT)
        schemaDirectiveSelection.convention(KobbyDirective.SELECTION)

        schemaTruncateReportEnabled.convention(false)
        schemaTruncateRegexEnabled.convention(false)
        schemaTruncateCaseSensitive.convention(true)

        truncatedSchema.convention(true)
        depth.convention(1)
        reportLengthLimit.convention(10_000)
        printMinWeight.convention(2)
        printOverride.convention(false)
        printArgumentTypes.convention(false)
        printSuperTypes.convention(false)
        printSubTypes.convention(false)
        regexEnabled.convention(false)
        caseSensitive.convention(true)
        query.convention(mapOf(KobbyTypeAlias.ROOT to KobbyTypeOperationQuery()))
    }

    @TaskAction
    fun printSchemaDependencyTree() {
        val graphQLSchemaFiles: List<File> = schemaFiles.get().map {
            it.asFile.absoluteFile.also { file ->
                if (!file.isFile) {
                    "Specified schema file does not exist: $it".throwIt()
                }
            }
        }

        if (graphQLSchemaFiles.isEmpty()) {
            "GraphQL schema files not found".throwIt()
        }

        val directiveLayout = mapOf(
            KobbyDirective.PRIMARY_KEY to schemaDirectivePrimaryKey.get(),
            KobbyDirective.REQUIRED to schemaDirectiveRequired.get(),
            KobbyDirective.DEFAULT to schemaDirectiveDefault.get(),
            KobbyDirective.SELECTION to schemaDirectiveSelection.get()
        )

        var schema = try {
            parseSchema(directiveLayout, *graphQLSchemaFiles.map { FileReader(it) }.toTypedArray())
        } catch (e: Exception) {
            "Schema parsing failed.".throwIt(e)
        }

        // Truncate schema
        val truncateQuery = schemaTruncateQuery.orNull
        if (truncatedSchema.get() && !truncateQuery.isNullOrEmpty()) {
            schema = try {
                schema.truncate(
                    reportEnabled = schemaTruncateReportEnabled.get(),
                    regexEnabled = schemaTruncateRegexEnabled.get(),
                    caseSensitive = schemaTruncateCaseSensitive.get(),
                    query = truncateQuery
                ) { message ->
                    logger.warn(message)
                }
            } catch (e: Exception) {
                "Schema truncation failed.".throwIt(e)
            }
        }

        logger.warn("")
        logger.warn(
            "[kobby] GraphQL schema ready to analyze!" +
                    reportQueryPropertiesStr(regexEnabled.get(), caseSensitive.get())
        )
        logger.warn(schema.reportWeightStr("[kobby] Analyzed schema"))
        logger.warn("")

        val pathSequence: Sequence<Path> = try {
            schema.analyze(
                depth.get(),
                regexEnabled.get(),
                caseSensitive.get(),
                query.get()
            )
        } catch (e: Exception) {
            "GraphQL schema analysis failed.".throwIt(e)
        }

        val limit: Int = reportLengthLimit.get()
        val withMinWeight: Int = printMinWeight.get()
        val withOverride: Boolean = printOverride.get()
        val withArgumentTypes: Boolean = printArgumentTypes.get()
        val withSuperTypes: Boolean = printSuperTypes.get()
        val withSubTypes: Boolean = printSubTypes.get()

        var counter = 0
        for (path in pathSequence) {
            counter++
            if (limit in 0 until counter) {
                logger.warn("")
                logger.warn("[kobby] Report length limit exceeded: $limit")
                break
            }
            logger.warn(
                path.toReportString(
                    withMinWeight,
                    withOverride,
                    withArgumentTypes,
                    withSuperTypes,
                    withSubTypes
                )
            )
        }
        if (counter == 0) {
            logger.warn("[kobby] Nothing found matching your analyse query.")
        }

        logger.warn("")
        logger.warn(schema.reportWeightStr("[kobby] Analyzed schema"))
        logger.warn("")
    }
}