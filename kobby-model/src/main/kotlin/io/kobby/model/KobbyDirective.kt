package io.kobby.model

/**
 * Created on 19.01.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyDirective(
    val rename: String = "rename",
    val required: String = "required",
    val default: String = "default"
) {
    companion object {
        const val RENAME_ARGUMENT = "name"
    }
}