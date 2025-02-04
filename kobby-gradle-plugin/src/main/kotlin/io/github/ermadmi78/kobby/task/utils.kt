package io.github.ermadmi78.kobby.task

import io.github.ermadmi78.kobby.model.KobbySchema
import io.github.ermadmi78.kobby.model.query.*
import org.gradle.api.tasks.TaskInstantiationException

/**
 * Created on 08.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

internal fun String.throwIt(cause: Throwable? = null): Nothing {
    val message = "[kobby] $this${if (cause == null) "" else " " + cause.message}"
    System.err.println(message)
    if (cause == null) {
        throw TaskInstantiationException(message)
    } else {
        throw TaskInstantiationException(message, cause)
    }
}

internal fun KobbySchema.truncate(
    reportEnabled: Boolean,
    regexEnabled: Boolean,
    caseSensitive: Boolean,
    query: Map<String, KobbyTypeOperationQuery>,
    print: (String) -> Unit
): KobbySchema {
    print("[kobby] GraphQL schema truncation enabled!" + reportQueryPropertiesStr(regexEnabled, caseSensitive))
    print(this.reportWeightStr("[kobby] Source schema"))

    val truncatedSchema: KobbySchema = this.truncate(regexEnabled, caseSensitive, query.toScope())

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
    query: Map<String, KobbyTypeOperationQuery>
): Sequence<Path> = this.analyze(depth, regexEnabled, caseSensitive, query.toScope())

private fun Map<String, KobbyTypeOperationQuery>.toScope(): KobbySchemaQueryScope.() -> Unit {
    val query = this
    return {
        query.forEach { (pattern, typeOperationQuery) ->
            forType(pattern) {
                include(typeOperationQuery.include.toScope())
                exclude(typeOperationQuery.exclude.toScope())
            }
        }
    }
}

private fun KobbyTypeQuery.toScope(): KobbyTypeQueryScope.() -> Unit {
    val query = this
    return {
        query.enumValue.forEach { pattern ->
            enumValue(pattern)
        }
        query.field.forEach { pattern ->
            field(pattern)
        }
        if (query.anyOverriddenField) {
            anyOverriddenField()
        }
        query.dependency.forEach { pattern ->
            dependency(pattern)
        }
        query.subTypeDependency.forEach { pattern ->
            subTypeDependency(pattern)
        }
        query.superTypeDependency.forEach { pattern ->
            superTypeDependency(pattern)
        }
        query.argumentDependency.forEach { pattern ->
            argumentDependency(pattern)
        }
        query.transitiveDependency.forEach { pattern ->
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