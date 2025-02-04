package io.github.ermadmi78.kobby

import io.github.ermadmi78.kobby.model.query.KobbyTypeAlias
import org.apache.maven.plugins.annotations.Parameter

/**
 * Created on 04.05.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

class QueryConfig {
    companion object {
        @JvmStatic
        private fun shift(): String = shift(8)
    }

    @Parameter
    var type: String = KobbyTypeAlias.ANY

    @Parameter
    var include: TypeQueryConfig? = null

    @Parameter
    var exclude: TypeQueryConfig? = null

    override fun toString(): String = "QueryConfig(" +
            "\n${shift()}  type=$type" +
            "\n${shift()}  include=$include" +
            "\n${shift()}  exclude=$exclude" +
            "\n${shift()})"
}

class TypeQueryConfig {
    companion object {
        @JvmStatic
        private fun shift(): String = shift(10)
    }

    @Parameter
    var enumValue: String? = null

    @Parameter
    var field: String? = null

    @Parameter
    var anyOverriddenField: Boolean = false

    @Parameter
    var dependency: String? = null

    @Parameter
    var subTypeDependency: String? = null

    @Parameter
    var superTypeDependency: String? = null

    @Parameter
    var argumentDependency: String? = null

    @Parameter
    var transitiveDependency: String? = null

    @Parameter
    var minWeight: Int? = null

    @Parameter
    var maxWeight: Int? = null

    override fun toString(): String = "TypeQueryConfig(" +
            "\n${shift()}  enumValue=$enumValue" +
            "\n${shift()}  field=$field" +
            "\n${shift()}  anyOverriddenField=$anyOverriddenField" +
            "\n${shift()}  dependency=$dependency" +
            "\n${shift()}  subTypeDependency=$subTypeDependency" +
            "\n${shift()}  superTypeDependency=$superTypeDependency" +
            "\n${shift()}  argumentDependency=$argumentDependency" +
            "\n${shift()}  transitiveDependency=$transitiveDependency" +
            "\n${shift()}  minWeight=$minWeight" +
            "\n${shift()}  maxWeight=$maxWeight" +
            "\n${shift()})"
}