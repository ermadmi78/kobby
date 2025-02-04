package io.github.ermadmi78.kobby.model.query

import io.github.ermadmi78.kobby.model.query.PatternKind.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/**
 * Created on 06.02.2025
 *
 * @author Dmitry Ermakov (ermadmi78@gmail.com)
 */
class KobbyPatternToRegexTest {
    @Test
    fun testSimpleKobbyPatternToRegex() {
        "" shouldBeRegex "^\$"
        "  " shouldBeRegex "^\$"
        "  #$%^^%~  $ " shouldBeRegex "^\$"

        "*" shouldBeRegex "^\\w*\$"
        " *" shouldBeRegex "^\\w*\$"
        "* " shouldBeRegex "^\\w*\$"
        " * " shouldBeRegex "^\\w*\$"

        "?" shouldBeRegex "^\\w?\$"
        " ?" shouldBeRegex "^\\w?\$"
        "? " shouldBeRegex "^\\w?\$"
        " ? " shouldBeRegex "^\\w?\$"

        "C" shouldBeRegex "^\\QC\\E\$"
        " C" shouldBeRegex "^\\QC\\E\$"
        "C " shouldBeRegex "^\\QC\\E\$"
        " C " shouldBeRegex "^\\QC\\E\$"

        "Cinema_15" shouldBeRegex "^\\QCinema_15\\E\$"
        " Cinema_15" shouldBeRegex "^\\QCinema_15\\E\$"
        "Cinema_15 " shouldBeRegex "^\\QCinema_15\\E\$"
        " Cinema_15 " shouldBeRegex "^\\QCinema_15\\E\$"
        "Ci ne  ma_15" shouldBeRegex "^\\QCinema_15\\E\$"

        "*Cinema_15" shouldBeRegex "^\\w*\\QCinema_15\\E\$"
        "Cinema_15*" shouldBeRegex "^\\QCinema_15\\E\\w*\$"
        "*Cinema_15*" shouldBeRegex "^\\w*\\QCinema_15\\E\\w*\$"

        "**Cinema_15" shouldBeRegex "^\\w*\\w*\\QCinema_15\\E\$"
        "Cinema_15**" shouldBeRegex "^\\QCinema_15\\E\\w*\\w*\$"
        "**Cinema_15**" shouldBeRegex "^\\w*\\w*\\QCinema_15\\E\\w*\\w*\$"

        "create*Cinema_15" shouldBeRegex "^\\Qcreate\\E\\w*\\QCinema_15\\E\$"
        "*create*Cinema_15" shouldBeRegex "^\\w*\\Qcreate\\E\\w*\\QCinema_15\\E\$"
        "create*Cinema_15*" shouldBeRegex "^\\Qcreate\\E\\w*\\QCinema_15\\E\\w*\$"
        "*create*Cinema_15*" shouldBeRegex "^\\w*\\Qcreate\\E\\w*\\QCinema_15\\E\\w*\$"
        "***create***Cinema_15***" shouldBeRegex "^\\w*\\w*\\w*\\Qcreate\\E\\w*\\w*\\w*\\QCinema_15\\E\\w*\\w*\\w*\$"

        "?Cinema_15" shouldBeRegex "^\\w?\\QCinema_15\\E\$"
        "Cinema_15?" shouldBeRegex "^\\QCinema_15\\E\\w?\$"
        "?Cinema_15?" shouldBeRegex "^\\w?\\QCinema_15\\E\\w?\$"

        "??Cinema_15" shouldBeRegex "^\\w?\\w?\\QCinema_15\\E\$"
        "Cinema_15??" shouldBeRegex "^\\QCinema_15\\E\\w?\\w?\$"
        "??Cinema_15??" shouldBeRegex "^\\w?\\w?\\QCinema_15\\E\\w?\\w?\$"

        "create?Cinema_15" shouldBeRegex "^\\Qcreate\\E\\w?\\QCinema_15\\E\$"
        "?create?Cinema_15" shouldBeRegex "^\\w?\\Qcreate\\E\\w?\\QCinema_15\\E\$"
        "create?Cinema_15?" shouldBeRegex "^\\Qcreate\\E\\w?\\QCinema_15\\E\\w?\$"
        "?create?Cinema_15?" shouldBeRegex "^\\w?\\Qcreate\\E\\w?\\QCinema_15\\E\\w?\$"
        "???create???Cinema_15???" shouldBeRegex "^\\w?\\w?\\w?\\Qcreate\\E\\w?\\w?\\w?\\QCinema_15\\E\\w?\\w?\\w?\$"
    }

    @Test
    fun testListKobbyPatternToRegex() {
        "|" shouldBeRegex "^\$"
        " |" shouldBeRegex "^\$"
        "| " shouldBeRegex "^\$"
        " | " shouldBeRegex "^\$"

        "||" shouldBeRegex "^\$"
        " ||" shouldBeRegex "^\$"
        "|| " shouldBeRegex "^\$"
        " || " shouldBeRegex "^\$"

        "Cinema_15|Cinema_7" shouldBeRegex "^\\QCinema_15\\E\$|^\\QCinema_7\\E\$"
        "|Cinema_15|Cinema_7" shouldBeRegex "^\\QCinema_15\\E\$|^\\QCinema_7\\E\$"
        "||Cinema_15||||Cinema_7" shouldBeRegex "^\\QCinema_15\\E\$|^\\QCinema_7\\E\$"
        " ||  Cin  ema_15  ||||  Cin ema_7   " shouldBeRegex "^\\QCinema_15\\E\$|^\\QCinema_7\\E\$"
        " ||  Cin  ema_15  ||||  Cine  ma_7   | | " shouldBeRegex "^\\QCinema_15\\E\$|^\\QCinema_7\\E\$|^\$"

        "*|*" shouldBeRegex "^\\w*\$|^\\w*\$"
        " || * || * " shouldBeRegex "^\\w*\$|^\\w*\$"
        " || * || * | | " shouldBeRegex "^\\w*\$|^\\w*\$|^\$"
        " || ** || ** | | " shouldBeRegex "^\\w*\\w*\$|^\\w*\\w*\$|^\$"

        "?|?" shouldBeRegex "^\\w?\$|^\\w?\$"
        " || ? || ? " shouldBeRegex "^\\w?\$|^\\w?\$"
        " || ? || ? | | " shouldBeRegex "^\\w?\$|^\\w?\$|^\$"
        " || ?? || ?? | | " shouldBeRegex "^\\w?\\w?\$|^\\w?\\w?\$|^\$"

        "create*Cinema_15|create*Cinema_7" shouldBeRegex
                "^\\Qcreate\\E\\w*\\QCinema_15\\E\$|^\\Qcreate\\E\\w*\\QCinema_7\\E\$"

        "create*Cinema?15|create*Cinema?7" shouldBeRegex
                "^\\Qcreate\\E\\w*\\QCinema\\E\\w?\\Q15\\E\$|^\\Qcreate\\E\\w*\\QCinema\\E\\w?\\Q7\\E\$"

        " create * Cinema ? 15 || create*Cinema ? 7   " shouldBeRegex
                "^\\Qcreate\\E\\w*\\QCinema\\E\\w?\\Q15\\E\$|^\\Qcreate\\E\\w*\\QCinema\\E\\w?\\Q7\\E\$"
    }

    private infix fun String.shouldBeRegex(expectedRegex: String) {
        assertEquals(expectedRegex, kobbyPatternToRegex(this))
    }

    @Test
    fun testKind() {
        "" shouldBeKind NOTHING
        "     " shouldBeKind NOTHING
        "|" shouldBeKind NOTHING
        " |||| || " shouldBeKind NOTHING
        " %$&^%& ||||&^^( || %%&^&" shouldBeKind NOTHING

        "Cinema_15" shouldBeKind TYPE
        " C in e ma_15 " shouldBeKind TYPE
        " ||| Cin ema_15 " shouldBeKind TYPE

        "Cinema_15|Cinema_7" shouldBeKind LIST_OF_TYPES
        " | Ci nem a_15 |||| Cine m a_7 |  " shouldBeKind LIST_OF_TYPES

        "*" shouldBeKind PATTERN
        " *** " shouldBeKind PATTERN

        "?" shouldBeKind PATTERN
        " ??? " shouldBeKind PATTERN

        "create*Cinema_15" shouldBeKind PATTERN
        " cr eate**Cinem a_15 " shouldBeKind PATTERN

        "create?Cinema_15" shouldBeKind PATTERN
        " cr eate??Cinem a_15 " shouldBeKind PATTERN

        "Cin*ema?15|Cin*ema_7" shouldBeKind PATTERN
        " | Cin****ema????15 |||| Cin***ema_7 ||||" shouldBeKind PATTERN
    }

    private infix fun String.shouldBeKind(expectedKind: PatternKind) {
        assertEquals(expectedKind, this.kind)
    }

    @Test
    fun testSplitToSet() {
        "" shouldBeSet emptySet()
        "     " shouldBeSet emptySet()
        "|" shouldBeSet emptySet()
        " |||| || " shouldBeSet emptySet()
        " %$&^%& ||||&^^( || %%&^&" shouldBeSet emptySet()

        "Cinema_15" shouldBeSet setOf("Cinema_15")
        " C in e ma_15 " shouldBeSet setOf("Cinema_15")
        " ||| Cin ema_15 " shouldBeSet setOf("Cinema_15")

        "Cinema_15|Cinema_7" shouldBeSet setOf("Cinema_15", "Cinema_7")
        " | Ci nem a_15 |||| Cine m a_7 |  " shouldBeSet setOf("Cinema_15", "Cinema_7")

        "*" shouldBeSet setOf("*")
        " *** " shouldBeSet setOf("***")

        "?" shouldBeSet setOf("?")
        " ??? " shouldBeSet setOf("???")

        "create*Cinema_15" shouldBeSet setOf("create*Cinema_15")
        " cr eate**Cinem a_15 " shouldBeSet setOf("create**Cinema_15")

        "create?Cinema_15" shouldBeSet setOf("create?Cinema_15")
        " cr eate??Cinem a_15 " shouldBeSet setOf("create??Cinema_15")

        "Cin*ema?15|Cin*ema_7" shouldBeSet setOf("Cin*ema?15", "Cin*ema_7")
        " | Cin****ema????15 |||| Cin***ema_7 ||||" shouldBeSet setOf("Cin****ema????15", "Cin***ema_7")

        " Cin* ema?15 | | || Ci n*em a_7 | Cin   ema_8 |||  " shouldBeSet
                setOf("Cin*ema?15", "Cin*ema_7", "Cinema_8")
    }

    private infix fun String.shouldBeSet(expectedSet: Set<String>) {
        assertEquals(expectedSet, this.splitToSet())
    }

    @Test
    fun testPack() {
        "" shouldByPackedSame ""
        " " shouldByPacked ""
        " %$&^%& &^^(  %%&^&" shouldByPacked ""

        "Cinema_15" shouldByPackedSame "Cinema_15"
        "  Ci nem $^%^$^ a_15 " shouldByPacked "Cinema_15"

        "Cin*ema?15|Cin*ema_7" shouldByPackedSame "Cin*ema?15|Cin*ema_7"
        " Ci n*e $$$$  m a? 15 |Ci n%%%%*e ma_  7 " shouldByPacked "Cin*ema?15|Cin*ema_7"

        "main" shouldByPackedSame "main"
    }

    private infix fun String.shouldByPacked(expected: String) {
        val actual = this.pack()
        assertNotEquals(this, actual)
        assertEquals(expected, actual)
    }

    private infix fun String.shouldByPackedSame(expected: String) {
        val actual = this.pack()
        assertSame(this, actual)
        assertEquals(expected, actual)
    }
}