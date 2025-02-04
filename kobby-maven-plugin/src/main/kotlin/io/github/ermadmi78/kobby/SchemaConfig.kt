package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.model.query.KobbyTypeAlias
import org.apache.maven.plugins.annotations.Parameter
import java.io.File

class SchemaConfig {
    @Parameter
    var files: List<File> = listOf()

    @Parameter
    var scan: ScanConfig = ScanConfig()

    @Parameter
    var directive: DirectiveConfig = DirectiveConfig()

    @Parameter
    var truncate: TruncateConfig? = null

    @Parameter
    var analyze: AnalyzeConfig = AnalyzeConfig()

    override fun toString(): String {
        return "SchemaConfig(" +
                "\n    files=${files.print(4)}, " +
                "\n    scan=$scan, " +
                "\n    directive=$directive" +
                "\n    truncate=$truncate" +
                "\n    analyze=$analyze" +
                "\n  )"
    }
}

class ScanConfig {
    @Parameter
    var dir: String = "src/main/resources"

    @Parameter
    var includes: List<String> = listOf("**/*.graphqls")

    @Parameter
    var excludes: List<String> = listOf()

    override fun toString(): String {
        return "ScanConfig(" +
                "\n      dir='$dir', " +
                "\n      includes=${includes.print(6)}, " +
                "\n      excludes=${excludes.print(6)}" +
                "\n    )"
    }
}

class DirectiveConfig {
    @Parameter
    var primaryKey: String = "primaryKey"

    @Parameter
    var required: String = "required"

    @Parameter
    var default: String = "default"

    @Parameter
    var selection: String = "selection"

    override fun toString(): String {
        return "DirectiveConfig(" +
                "\n      primaryKey=$primaryKey, " +
                "\n      required=$required, " +
                "\n      default=$default, " +
                "\n      selection=$selection" +
                "\n    )"
    }
}

class TruncateConfig {
    @Parameter
    var reportEnabled: Boolean = false

    @Parameter
    var regexEnabled: Boolean = false

    @Parameter
    var caseSensitive: Boolean = true

    @Parameter
    var queries: List<QueryConfig> = listOf()

    override fun toString(): String {
        return "TruncateConfig(" +
                "\n      reportEnabled=$reportEnabled, " +
                "\n      regexEnabled=$regexEnabled, " +
                "\n      caseSensitive=$caseSensitive, " +
                "\n      queries=${queries.print(6)}" +
                "\n    )"
    }
}

class AnalyzeConfig {
    @Parameter
    var truncatedSchema: Boolean = true

    @Parameter
    var depth: Int = 1

    @Parameter
    var reportLengthLimit: Int = 10000

    @Parameter
    var printMinWeight: Int = 2

    @Parameter
    var printOverride: Boolean = false

    @Parameter
    var printArgumentTypes: Boolean = false

    @Parameter
    var printSuperTypes: Boolean = false

    @Parameter
    var printSubTypes: Boolean = false

    @Parameter
    var regexEnabled: Boolean = false

    @Parameter
    var caseSensitive: Boolean = true

    @Parameter
    var queries: List<QueryConfig> = listOf(
        QueryConfig().also { it.type = KobbyTypeAlias.ROOT }
    )

    override fun toString(): String {
        return "TruncateConfig(" +
                "\n      truncatedSchema=$truncatedSchema, " +
                "\n      depth=$depth, " +
                "\n      reportLengthLimit=$reportLengthLimit, " +
                "\n      printMinWeight=$printMinWeight, " +
                "\n      printOverride=$printOverride, " +
                "\n      printArgumentTypes=$printArgumentTypes, " +
                "\n      printSuperTypes=$printSuperTypes, " +
                "\n      printSubTypes=$printSubTypes, " +
                "\n      regexEnabled=$regexEnabled, " +
                "\n      caseSensitive=$caseSensitive, " +
                "\n      queries=${queries.print(6)}" +
                "\n    )"
    }
}