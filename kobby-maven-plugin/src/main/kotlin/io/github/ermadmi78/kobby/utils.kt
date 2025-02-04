package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.query.*
import org.codehaus.plexus.util.DirectoryScanner
import java.io.File

/**
 * Created on 04.05.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

internal fun shift(shift: Int): String = buildString {
    shift(shift)
}

internal fun StringBuilder.shift(shift: Int): StringBuilder {
    for (i in 1..shift) {
        append(' ')
    }
    return this
}

internal fun <T : Any> List<T>.print(shift: Int): String = buildString {
    if (this@print.isEmpty()) {
        append("[]")
    } else {
        append('[')
        this@print.forEach { file ->
            append('\n').shift(shift + 2).append(file)
        }
        append('\n').shift(shift).append(']')
    }
}

internal fun File.scanGraphqls(scan: ScanConfig): Sequence<File> = DirectoryScanner().run {
    basedir = this@scanGraphqls
    setIncludes(scan.includes.toTypedArray())
    setExcludes(scan.excludes.toTypedArray())
    setFollowSymlinks(false)
    scan()
    includedFiles
}.asSequence().map { File(this, it).absoluteFile }.filter { it.isFile }

internal fun KobbySchema.truncate(
    reportEnabled: Boolean,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    queries: List<QueryConfig>,
    print: (String) -> Unit
): KobbySchema {
    print("[kobby] GraphQL schema truncation enabled!" + reportQueryPropertiesStr(regexEnabled, caseSensitive))
    print(this.reportWeightStr("[kobby] Source schema"))

    val truncatedSchema: KobbySchema = this.truncate(regexEnabled, caseSensitive, queries.toScope())

    val report = TruncationReport(this, truncatedSchema)

    if (reportEnabled) {
        print("")
        print("[kobby] Detailed truncation report ****************************************************")
        report.detailedReport().forEach(print)
        print("")
        print("[kobby] *******************************************************************************")
        print("")
    }

    print(report.truncationCompletedStr("[kobby]"))
    print(truncatedSchema.reportWeightStr("[kobby] Truncated schema"))

    return truncatedSchema
}

internal fun KobbySchema.analyze(
    depth: Int,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    queries: List<QueryConfig>
): Sequence<Path> = this.analyze(depth, regexEnabled, caseSensitive, queries.toScope())

private fun List<QueryConfig>.toScope(): KobbySchemaQueryScope.() -> Unit {
    val queries = this
    return {
        queries.forEach { query: QueryConfig ->
            forType(query.type) {
                query.include?.also { typeQuery: TypeQueryConfig ->
                    include(typeQuery.toScope())
                }
                query.exclude?.also { typeQuery: TypeQueryConfig ->
                    exclude(typeQuery.toScope())
                }
            }
        }
    }
}

private fun TypeQueryConfig.toScope(): KobbyTypeQueryScope.() -> Unit {
    val query = this
    return {
        query.enumValue?.also { pattern ->
            enumValue(pattern)
        }
        query.field?.also { pattern ->
            field(pattern)
        }
        if (query.anyOverriddenField) {
            anyOverriddenField()
        }
        query.dependency?.also { pattern ->
            dependency(pattern)
        }
        query.subTypeDependency?.also { pattern ->
            subTypeDependency(pattern)
        }
        query.superTypeDependency?.also { pattern ->
            superTypeDependency(pattern)
        }
        query.argumentDependency?.also { pattern ->
            argumentDependency(pattern)
        }
        query.transitiveDependency?.also { pattern ->
            transitiveDependency(pattern)
        }
        query.minWeight?.also {
            minWeight(it)
        }
        query.maxWeight?.also {
            maxWeight(it)
        }
    }
}