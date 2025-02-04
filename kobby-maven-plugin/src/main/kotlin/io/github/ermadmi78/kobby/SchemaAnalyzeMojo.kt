package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.model.KobbyDirective
import io.github.ermadmi78.kobby.model.parseSchema
import io.github.ermadmi78.kobby.model.query.Path
import io.github.ermadmi78.kobby.model.query.reportQueryPropertiesStr
import io.github.ermadmi78.kobby.model.query.reportWeightStr
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.io.File
import java.io.FileReader

/**
 * To run Mojo use:
 * ```
 * mvn io.github.ermadmi78:kobby-maven-plugin:schema-analyze
 * ```
 *
 * Created on 06.05.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@Mojo(name = "schema-analyze")
class SchemaAnalyzeMojo : AbstractMojo() {
    /**
     * The current Maven project.
     */
    @Parameter(property = "project", required = true, readonly = true)
    private lateinit var project: MavenProject

    @Parameter
    private var schema: SchemaConfig = SchemaConfig()

    override fun execute() {
        if (log.isDebugEnabled) {
            log.debug("[Kobby] Plugin Configuration:\n$this")
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

        val directiveLayout = mapOf(
            KobbyDirective.PRIMARY_KEY to schema.directive.primaryKey,
            KobbyDirective.REQUIRED to schema.directive.required,
            KobbyDirective.DEFAULT to schema.directive.default,
            KobbyDirective.SELECTION to schema.directive.selection
        )

        var kobbySchema = try {
            parseSchema(directiveLayout, *schema.files.map { FileReader(it) }.toTypedArray())
        } catch (e: Exception) {
            "Schema parsing failed.".throwIt(e)
        }

        val analyzeConfig: AnalyzeConfig = schema.analyze
        val truncateConfig: TruncateConfig? = schema.truncate

        if (analyzeConfig.truncatedSchema && truncateConfig != null) {
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

        log.info("")
        log.info(
            "[kobby] GraphQL schema ready to analyze!" +
                    reportQueryPropertiesStr(analyzeConfig.regexEnabled, analyzeConfig.caseSensitive)
        )
        log.info(kobbySchema.reportWeightStr("[kobby] Analyzed schema"))
        log.info("")

        val pathSequence: Sequence<Path> = try {
            kobbySchema.analyze(
                analyzeConfig.depth,
                analyzeConfig.regexEnabled,
                analyzeConfig.caseSensitive,
                analyzeConfig.queries
            )
        } catch (e: Exception) {
            "GraphQL schema analysis failed.".throwIt(e)
        }

        var counter = 0
        for (path in pathSequence) {
            counter++
            if (analyzeConfig.reportLengthLimit in 0 until counter) {
                log.info("")
                log.info("[kobby] Report length limit exceeded: ${analyzeConfig.reportLengthLimit}")
                break
            }
            log.info(
                path.toReportString(
                    analyzeConfig.printMinWeight,
                    analyzeConfig.printOverride,
                    analyzeConfig.printArgumentTypes,
                    analyzeConfig.printSuperTypes,
                    analyzeConfig.printSubTypes
                )
            )
        }
        if (counter == 0) {
            log.info("[kobby] Nothing found matching your analyse query.")
        }

        log.info("")
        log.info(kobbySchema.reportWeightStr("[kobby] Analyzed schema"))
        log.info("")
    }

    override fun toString(): String {
        return "SchemaAnalyzeMojo(" +
                "\n  schema=$schema" +
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