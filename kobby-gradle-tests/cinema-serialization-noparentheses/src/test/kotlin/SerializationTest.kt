import ContextHolder.context
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.runBlocking

class SerializationTest : AnnotationSpec() {

    @Test
    fun `query request`() = runBlocking {
        val result = context.query {
            films {
                limit = 5
                title
            }
        }

        result.films shouldHaveSize 5
    }
}
