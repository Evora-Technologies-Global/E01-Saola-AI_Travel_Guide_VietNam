package com.evora.technologies.saola.tablet.feature.journal

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.evora.technologies.saola.core.designsystem.theme.SaolaTheme
import com.evora.technologies.saola.domain.model.JournalDay
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.feature.collection.CollectionState
import com.evora.technologies.saola.feature.journal.JournalState
import com.evora.technologies.saola.feature.passport.PassportState
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_title
import com.evora.technologies.saola.tablet.WINDOW_HEIGHT
import com.evora.technologies.saola.tablet.WINDOW_WIDTH
import com.evora.technologies.saola.tablet.startsAtTheLeadingEdge
import com.evora.technologies.saola.testing.discovery
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The day column keeps its place when the pane beside it is swapped.
 *
 * This is the master–detail promise the journal makes on a large window, and the design note
 * states it as the reason for the arrangement: *"the list of days stays put while the passport
 * map fills the rest."* A traveller who has scrolled back through a week, taps Collection to
 * check what they have found, and taps Passport again, must land on the same week — otherwise
 * the pane switch costs them their place every time they use it, and they stop using it.
 *
 * **Written as a regression guard, not as a discovery.** The screen already passes: the day
 * column never leaves composition, so even a `rememberLazyListState()` down inside it would
 * survive today. `JournalTabletScreen` hoists the state above the pane switch anyway, and its
 * comment says why — the first person to wrap the two panes in an `AnimatedContent`, which is
 * exactly what `DiscoveryTabletScreen` does two packages away, would break this with nothing
 * to notice. This test is that something.
 *
 * **Run it on a large-window device.** Green on the Pixel Tablet AVD at API 35 on 04.08.2026,
 * with the whole module's suite — 12 of 12. It will not run on API 37, where `Espresso.onIdle`
 * dies in `InputManager.getInstance` before any test body executes; that is `LLM.md` §11 row
 * #18 and it belongs to the platform, not to this test.
 */
class JournalPaneSwitchTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun switchingTheRightPaneLeavesTheDayColumnWhereItWas() {
        // Resolved inside the composition, so the test reads the same string the screen draws
        // rather than a copy of the English one — these run on whatever locale the device is in.
        var collectionLabel = ""

        compose.setContent {
            collectionLabel = stringResource(Res.string.collection_title)
            SaolaTheme {
                JournalTabletScreen(
                    state = journalState(),
                    passportState = PassportState(isLoading = false),
                    collectionState = CollectionState(isLoading = false),
                    today = NEWEST_DAY,
                    initialPane = JournalPane.PASSPORT,
                    hasLocationPermission = true,
                    isPermissionBlocked = false,
                    onIntent = {},
                    onPassportIntent = {},
                    onCollectionIntent = {},
                    onRequestPermission = {},
                    onOpenDiscovery = {},
                    onOpenLens = {},
                    onOpenSovereignty = {},
                    modifier = Modifier.requiredSize(WINDOW_WIDTH, WINDOW_HEIGHT),
                )
            }
        }
        compose.waitForIdle()

        // The right-hand edge of the day column, read off the arrangement rather than typed as
        // a pixel count, so the row lookup below stays right at any density.
        val dayColumnRight = dayColumn().fetchSemanticsNode().boundsInRoot.right

        /**
         * The Collection row *in the day column* — not the pane's own title, which carries the
         * same string once the pane is open. `ProgressRow` merges its descendants into one
         * clickable node, so this is the row itself: the thing that is tapped and the thing
         * whose top edge moves when the list scrolls.
         */
        fun collectionRow() = compose.onAllNodesWithText(collectionLabel).filterToOne(
            SemanticsMatcher("is inside the day column") { it.boundsInRoot.left < dayColumnRight },
        )

        fun collectionRowTop(): Float = collectionRow().fetchSemanticsNode().boundsInRoot.top

        val atRest = collectionRowTop()

        // A short scroll, and deliberately so: the switch the traveller is about to use is
        // itself an item near the top of this list, so a scroll long enough to hide it would be
        // testing an interaction nobody can perform. This is how the phase-06 hand check was
        // done too — nudged off the top, then tapped.
        //
        // Half of where the row already sits, rather than a constant: `ScrollBy` takes device
        // pixels, so any fixed number means a different fraction of the list at every density —
        // and at a low enough one it would push the row out of the viewport, at which point the
        // click below fails with "could not find any node" instead of a wrong position. Derived
        // from the same measurement it has to stay inside, it cannot.
        val scrollBy = atRest / 2f
        dayColumn().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, scrollBy) }
        compose.waitForIdle()

        val scrolled = collectionRowTop()
        assertTrue(
            "the day column did not move: the row sat at $atRest and is now at $scrolled after " +
                "a $scrollBy px scroll. The assertion below would then hold for a list reset to " +
                "the top as well.",
            atRest - scrolled > scrollBy / 2f,
        )

        // The switch itself: the Collection row is the same `ProgressRow` the phone draws — on a
        // phone it navigates away, here it moves the pane beside the days.
        collectionRow().performClick()
        compose.waitForIdle()

        // The control. Without it this test would pass just as happily if the click had missed
        // and nothing had changed, which is the failure mode a position assertion is least able
        // to see: a column that never moved is indistinguishable from one that held its place.
        assertEquals(
            "the collection pane did not open — the title appears once in the day column's " +
                "ProgressRow and a second time in the pane's own PageHeader once it is there",
            2,
            compose.onAllNodesWithText(collectionLabel).fetchSemanticsNodes().size,
        )

        assertEquals(
            "the day column moved when the pane beside it was swapped",
            scrolled,
            collectionRowTop(),
            TOLERANCE_PX,
        )
    }

    private fun dayColumn() =
        compose.onAllNodes(hasScrollAction()).filterToOne(startsAtTheLeadingEdge)

    /**
     * Twelve days, each holding one find.
     *
     * Enough rows that the column scrolls well past a screenful at 800 dp, which is the only
     * property the test needs from the data. Dates run backwards from [NEWEST_DAY] within one
     * month so they can be written out rather than computed — a date arithmetic helper here
     * would be a second thing that could be wrong.
     */
    private fun journalState() = JournalState(
        isLoading = false,
        days = List(DAY_COUNT) { index ->
            JournalDay(
                date = LocalDate(YEAR, MONTH, NEWEST_DAY_OF_MONTH - index),
                discoveries = listOf(discovery("d$index")),
                summary = null,
            )
        },
        stats = JournalStats(
            totalDiscoveries = DAY_COUNT,
            totalDays = DAY_COUNT,
            favoriteCount = 0,
            categoryBreakdown = emptyMap(),
        ),
        provincesUnlocked = 3,
        provincesTotal = 34,
        itemsCollected = 2,
        itemsTotal = 40,
    )

    private companion object {
        const val YEAR = 2026
        const val MONTH = 7
        const val NEWEST_DAY_OF_MONTH = 20

        /** Twelve days back from the 20th stays inside the month, so no date arithmetic. */
        const val DAY_COUNT = 12
        val NEWEST_DAY = LocalDate(YEAR, MONTH, NEWEST_DAY_OF_MONTH)

        /** Two reads of the same row's top edge; a pixel apart is rounding, not movement. */
        const val TOLERANCE_PX = 1f
    }
}
