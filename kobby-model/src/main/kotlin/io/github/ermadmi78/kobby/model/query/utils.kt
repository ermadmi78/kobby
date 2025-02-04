package io.github.ermadmi78.kobby.model.query

enum class PatternKind {
    NOTHING,
    TYPE,
    LIST_OF_TYPES,
    PATTERN
}

val CharSequence.kind: PatternKind
    get() {
        var res = PatternKind.NOTHING
        for (c in this) {
            when {
                c.isLetter() || c.isDigit() || c == '_' -> {
                    if (res == PatternKind.NOTHING) {
                        res = PatternKind.TYPE
                    }
                }

                c == '|' -> {
                    if (res == PatternKind.TYPE) {
                        res = PatternKind.LIST_OF_TYPES
                    }
                }

                c == '*' || c == '?' -> {
                    return PatternKind.PATTERN
                }
            }
        }

        return res
    }

fun kobbyPatternToRegex(pattern: CharSequence): String = buildString {
    append('^')

    var wasOr = true
    var wasSeparator = true
    for (c in pattern) {
        when (c) {
            '|' -> {
                if (!wasSeparator) {
                    append('\\').append('E')
                    wasSeparator = true
                }

                if (!wasOr) {
                    append('$')
                    append('|')
                    append('^')
                    wasOr = true
                }
            }

            '*' -> {
                if (!wasSeparator) {
                    append('\\').append('E')
                    wasSeparator = true
                }

                append('\\').append('w').append('*')
                wasOr = false
            }

            '?' -> {
                if (!wasSeparator) {
                    append('\\').append('E')
                    wasSeparator = true
                }

                append('\\').append('w').append('?')
                wasOr = false
            }

            else -> {
                if (c.isLetter() || c.isDigit() || c == '_') {
                    if (wasSeparator) {
                        append('\\').append('Q')
                        wasSeparator = false
                    }
                    append(c)
                    wasOr = false
                }
            }
        }
    }

    if (!wasSeparator) {
        append('\\').append('E')
    }
    append('$')
}

fun CharSequence.splitToSet(): Set<String> {
    val set = mutableSetOf<String>()

    val item = StringBuilder()
    for (c in this) {
        when {
            c.isLetter() || c.isDigit() || c == '_' || c == '*' || c == '?' -> {
                item.append(c)
            }

            c == '|' -> {
                if (item.isNotEmpty()) {
                    set += item.toString()
                    item.clear()
                }
            }
        }
    }

    if (item.isNotEmpty()) {
        set += item.toString()
    }

    return set
}

fun String.pack(): String {
    var valid = true
    for (c in this) {
        if (!c.valid) {
            valid = false
            break
        }
    }

    if (valid) {
        return this
    }

    val source = this
    return buildString {
        for (c in source) {
            if (c.valid) {
                append(c)
            }
        }
    }
}

private val Char.valid: Boolean
    get() = this.isLetter() || this.isDigit() || this == '_' || this == '*' || this == '?' || this == '|'