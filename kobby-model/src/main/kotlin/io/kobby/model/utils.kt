package io.kobby.model

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class Decoration(val prefix: String?, val postfix: String?)

fun String.decorate(decoration: Decoration): String =
    decorate(decoration.prefix, decoration.postfix)

fun String.decorate(prefix: String?, postfix: String?): String {
    return if (prefix.isNullOrBlank() && postfix.isNullOrBlank()) {
        this
    } else if (prefix.isNullOrBlank()) {
        this + postfix!!.trim()
    } else if (postfix.isNullOrBlank()) {
        prefix.trim() + this.capitalize()
    } else {
        prefix.trim() + this.capitalize() + postfix.trim()
    }
}

fun String?.isIdentifier(): Boolean {
    if (isNullOrEmpty()) {
        return false
    }

    for (i in 0 until length) {
        val c = get(i)
        if (i == 0) {
            if (!c.isJavaIdentifierStart()) {
                return false
            }
        } else {
            if (!c.isJavaIdentifierPart()) {
                return false
            }
        }
    }

    return true
}