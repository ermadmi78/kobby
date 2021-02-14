package io.github.ermadmi78.kobby.model

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.TYPEALIAS

/**
 * Created on 20.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
@DslMarker
@Target(CLASS, TYPEALIAS)
annotation class KobbyScope
