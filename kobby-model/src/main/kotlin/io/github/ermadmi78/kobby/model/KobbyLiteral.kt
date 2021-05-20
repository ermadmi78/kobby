package io.github.ermadmi78.kobby.model

import java.math.BigDecimal
import java.math.BigInteger


/**
 * Created on 20.05.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

sealed class KobbyLiteral

object KobbyNullLiteral : KobbyLiteral() {
    override fun toString(): String = "null"
}

class KobbyBooleanLiteral(val value: Boolean) : KobbyLiteral() {
    override fun toString(): String = value.toString()
}

class KobbyIntLiteral(val value: BigInteger) : KobbyLiteral() {
    override fun toString(): String = value.toString()
}

class KobbyFloatLiteral(val value: BigDecimal) : KobbyLiteral() {
    override fun toString(): String = value.toString()
}

class KobbyStringLiteral(val value: String) : KobbyLiteral() {
    override fun toString(): String = "\"$value\""
}

class KobbyEnumLiteral(val name: String) : KobbyLiteral() {
    override fun toString(): String = name
}

class KobbyListLiteral(val values: List<KobbyLiteral>) : KobbyLiteral() {
    override fun toString(): String = values.joinToString(prefix = "[", postfix = "]")
}

class KobbyObjectLiteral(val values: Map<String, KobbyLiteral>) : KobbyLiteral() {
    override fun toString(): String = values.asSequence().joinToString(prefix = "{", postfix = "}") {
        "\"${it.key}\": ${it.value}"
    }
}

class KobbyVariableLiteral(val name: String) : KobbyLiteral() {
    override fun toString(): String = "$$name"
}