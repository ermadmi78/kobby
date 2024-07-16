package io.github.ermadmi78.kobby.generator.kotlin

/**
 * Created on 17.11.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

internal fun String.escape() = if (isKeyword) "`$this`" else this

private val String.isKeyword get() = this in KEYWORDS

// https://kotlinlang.org/docs/reference/keyword-reference.html
private val KEYWORDS = setOf(
    // Hard keywords
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "interface",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",

    // Soft keywords
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",

    // Modifier keywords
    "actual",
    "abstract",
    "annotation",
    "companion",
    "const",
    "crossinline",
    "data",
    "enum",
    "expect",
    "external",
    "final",
    "infix",
    "inline",
    "inner",
    "internal",
    "lateinit",
    "noinline",
    "open",
    "operator",
    "out",
    "override",
    "private",
    "protected",
    "public",
    "reified",
    "sealed",
    "suspend",
    "tailrec",
    "value",
    "vararg",
)

internal val FORBIDDEN_ENUM_NAMES = setOf("name", "ordinal")

internal fun String.rename(occupied: Set<String>): String {
    for (i in 2..100) {
        val renamed = "_".repeat(i) + this
        if (renamed !in occupied) {
            return renamed
        }
    }

    error("Cannot rename '$this' to avoid conflict with $occupied")
}

// All the methods of java.lang.Object
internal val FORBIDDEN_PROJECTION_NAMES = setOf(
    "getClass",
    "hashCode",
    "equals",
    "clone",
    "toString",
    "notify",
    "notifyAll",
    "wait",
    "finalize"
)