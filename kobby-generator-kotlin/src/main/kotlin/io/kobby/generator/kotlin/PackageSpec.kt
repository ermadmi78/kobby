package io.kobby.generator.kotlin

import java.lang.Character.isJavaIdentifierPart
import java.lang.Character.isJavaIdentifierStart

/**
 * Created on 18.11.2020
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class PackageSpec(packageName: String, prefix: PackageSpec? = null) {
    val path: List<String> = prefix?.let {
        it.path + packageName.toPath()
    } ?: packageName.toPath()

    val name: String by lazy {
        path.joinToString(".")
    }

    operator fun plus(packageName: String): PackageSpec =
        PackageSpec(packageName, this)

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }

        other as PackageSpec
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }

    override fun toString(): String {
        return name
    }

    private fun String.toPath(): List<String> = splitToSequence('.')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .onEach {
            require(it.isIdentifier()) {
                "Invalid package name [$this] - [$it] is not identifier"
            }
        }.toList()

    private fun String.isIdentifier(): Boolean {
        if (isEmpty()) {
            return false
        }
        for (i in indices) {
            val c = get(i)
            if (i == 0) {
                if (!isJavaIdentifierStart(c)) {
                    return false
                }
            } else {
                if (!isJavaIdentifierPart(c)) {
                    return false
                }
            }
        }
        return true
    }
}