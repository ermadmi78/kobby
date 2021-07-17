package io.github.ermadmi78.kobby

import org.apache.maven.plugins.annotations.Parameter
import java.io.File

class SchemaConfig {
    @Parameter
    var files: List<File> = listOf()

    @Parameter
    var scan: ScanConfig = ScanConfig()

    @Parameter
    var directive: DirectiveConfig = DirectiveConfig()

    override fun toString(): String {
        return "SchemaConfig(" +
                "\n    files=${files.print(4)}, " +
                "\n    scan=$scan, " +
                "\n    directive=$directive" +
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

    @Parameter
    var resolve: String = "resolve"

    override fun toString(): String {
        return "DirectiveConfig(" +
                "\n      primaryKey=$primaryKey, " +
                "\n      required=$required, " +
                "\n      default=$default, " +
                "\n      selection=$selection, " +
                "\n      resolve=$resolve" +
                "\n    )"
    }
}

private fun <T : Any> List<T>.print(shift: Int): String = buildString {
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