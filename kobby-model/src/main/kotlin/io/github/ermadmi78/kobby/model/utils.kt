package io.github.ermadmi78.kobby.model

import java.io.File
import java.util.*

/**
 * Created on 18.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
data class Decoration(val prefix: String?, val postfix: String?)

fun Decoration.isEmpty(): Boolean = prefix.isNullOrEmpty() && postfix.isNullOrEmpty()

fun Decoration.isNotEmpty(): Boolean = !isEmpty()

fun String._capitalize(): String =
    if (isEmpty() || this[0].isUpperCase()) this
    else replaceFirstChar { it.titlecase(Locale.getDefault()) }

fun String._decapitalize(): String =
    if (isEmpty() || this[0].isLowerCase()) this
    else replaceFirstChar { it.lowercase(Locale.getDefault()) }

fun String.decorate(decoration: Decoration): String =
    decorate(decoration.prefix, decoration.postfix)

fun String.decorate(prefix: String?, postfix: String?): String {
    return if (prefix.isNullOrBlank() && postfix.isNullOrBlank()) {
        this
    } else if (prefix.isNullOrBlank()) {
        this + postfix!!.trim()
    } else if (postfix.isNullOrBlank()) {
        prefix.trim() + this._capitalize()
    } else {
        prefix.trim() + this._capitalize() + postfix.trim()
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

internal fun <K : Any, V : Any> MutableMap<K, MutableSet<V>>.append(key: K, value: V) =
    computeIfAbsent(key) { mutableSetOf() }.add(value)

object PluginUtils {
    val File.contextName: String?
        get() = name
            .splitToSequence('.')
            .filter { it.isNotBlank() }
            .firstOrNull()
            ?._decapitalize()

    fun String.pathIterator(): Iterator<String> =
        splitToSequence('/', '\\')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .iterator()

    fun List<Iterator<String>>.extractCommonPrefix(): Iterator<String> = iterator<String> {
        while (true) {
            var element: String? = null
            for (iterator in this@extractCommonPrefix) {
                if (!iterator.hasNext()) {
                    return@iterator
                }

                val cur = iterator.next()
                if (element == null) {
                    element = cur
                } else if (element != cur) {
                    return@iterator
                }
            }

            if (element == null) {
                return@iterator
            }

            yield(element)
        }
    }

    fun <T : Any> Iterator<T>.removePrefixOrEmpty(prefix: Iterator<T>): Iterator<T> = iterator {
        for (prefixElement in prefix) {
            if (!this@removePrefixOrEmpty.hasNext()) {
                return@iterator
            }
            if (prefixElement != this@removePrefixOrEmpty.next()) {
                return@iterator
            }
        }

        for (element in this@removePrefixOrEmpty) {
            yield(element)
        }
    }

    fun String.forEachPackage(action: (String) -> Unit) =
        this.splitToSequence('.')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach(action)

    fun List<String>.toPackageName(): String = joinToString(".") { it }
}