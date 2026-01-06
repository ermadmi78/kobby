import kobby.kotlin.entity.Actor
import kobby.kotlin.entity.ActorProjection
import java.time.LocalDate

/**
 * Created on 21.05.2021
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */

suspend fun Actor.refresh(id: Long, __projection: (ActorProjection.() -> Unit)? = null): Actor =
    __context().fetchActor(id) {
        __projection?.invoke(this) ?: __withCurrentProjection()
    }

suspend fun Actor.addFilm(filmId: Long): Boolean = __context().mutation {
    associate(filmId, id)
}.associate

suspend fun Actor.tag(tagValue: String): Boolean = __context().mutation {
    tagActor(id, tagValue)
}.tagActor

suspend fun Actor.updateBirthday(
    birthday: LocalDate,
    __projection: (ActorProjection.() -> Unit)? = null
): Actor = __context().updateBirthday(id, birthday) {
    __projection?.invoke(this) ?: __withCurrentProjection()
} ?: error("Cannot find actor by id = $id")