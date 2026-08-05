package com.duylt.trave.vietlensai.tablet.feature.discovery

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performSemanticsAction
import com.duylt.trave.vietlensai.core.designsystem.theme.VietLensTheme
import com.duylt.trave.vietlensai.domain.model.ChatMessage
import com.duylt.trave.vietlensai.domain.model.ChatRole
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.DiscoverySection
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.feature.chat.ChatState
import com.duylt.trave.vietlensai.feature.discovery.DiscoveryState
import com.duylt.trave.vietlensai.tablet.WINDOW_HEIGHT
import com.duylt.trave.vietlensai.tablet.WINDOW_WIDTH
import com.duylt.trave.vietlensai.tablet.startsAtTheLeadingEdge
import com.duylt.trave.vietlensai.tablet.scrollOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

/**
 * The story stays where the traveller left it while the guide beside it works.
 *
 * This is the whole argument for the discovery's large-window arrangement — *"you read the
 * story and question it at the same time"* — and it is only true if asking a question does not
 * cost the reader their place in the article. On a phone the guide is a screen away, so the
 * question never arose; here the two panes are alive at once and the story is the pane that
 * must not move.
 *
 * Two tests, because the property has two ways of breaking and only one of them is the obvious
 * one. [askingTheGuideLeavesTheStoryWhereItWas] is what the plan asked for and what a reviewer
 * would think of. [aFullWindowPageLeavesTheStoryWhereItWas] is the one that would actually go
 * red if `storyScroll` were moved back down into `StoryPane`, because `DiscoveryTabletScreen`
 * wraps its panes in an `AnimatedContent` and a scroll state declared inside that lambda is
 * disposed with the page.
 *
 * **Run it on a large-window device.** Green on the Pixel Tablet AVD at API 35 on 04.08.2026,
 * with the whole module's suite — 12 of 12. It will not run on API 37, where `Espresso.onIdle`
 * dies in `InputManager.getInstance` before any test body executes; that is `LLM.md` §11 row
 * #18 and it belongs to the platform, not to this test.
 */
class DiscoveryStoryScrollTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * The guide answering a question must not disturb the article.
     *
     * The exchange is driven through `chatState` rather than by typing into the composer,
     * because what the story pane reacts to is the state change, not the keystrokes — and a
     * test that typed would be measuring the soft keyboard's effect on the window as much as
     * anything else.
     */
    @Test
    fun askingTheGuideLeavesTheStoryWhereItWas() {
        var chatState by mutableStateOf(ChatState(discovery = DISCOVERY))
        setContent(discoveryState = { DiscoveryState(isLoading = false, discovery = DISCOVERY) }) {
            chatState
        }

        val before = scrollTheStory()

        // The question goes up and the thinking card appears.
        chatState = chatState.copy(messages = listOf(message("q1", ChatRole.USER)), isSending = true)
        compose.waitForIdle()
        // The answer lands and the card goes.
        chatState = chatState.copy(
            messages = listOf(message("q1", ChatRole.USER), message("a1", ChatRole.ASSISTANT)),
            isSending = false,
        )
        compose.waitForIdle()

        assertEquals(
            "the story pane scrolled while the guide beside it was answering",
            before,
            storyPane().scrollOffset(),
            TOLERANCE,
        )
    }

    /**
     * A page that takes the whole window and hands it back must give the story back too.
     *
     * Three things do this — the in-note viewfinder, the photo viewer and the analysing state
     * — and all three go through the same `AnimatedContent`, so one of them is enough to
     * exercise the risk. The analysing page is chosen because it is the only one of the three a
     * test can reach by setting a field, rather than by driving a camera or a photo album.
     */
    @Test
    fun aFullWindowPageLeavesTheStoryWhereItWas() {
        var state by mutableStateOf(DiscoveryState(isLoading = false, discovery = DISCOVERY))
        setContent(discoveryState = { state }) { ChatState(discovery = DISCOVERY) }

        val before = scrollTheStory()

        state = state.copy(isLoading = true)
        compose.waitForIdle()
        state = state.copy(isLoading = false)
        compose.waitForIdle()

        assertEquals(
            "the story pane went back to the top after a full-window page closed — the scroll " +
                "state has moved inside the AnimatedContent, where it is disposed with the page",
            before,
            storyPane().scrollOffset(),
            TOLERANCE,
        )
    }

    // ---------------------------------------------------------------------------------

    private fun setContent(
        discoveryState: () -> DiscoveryState,
        chatState: () -> ChatState,
    ) {
        compose.setContent {
            VietLensTheme {
                DiscoveryTabletScreen(
                    state = discoveryState(),
                    chatState = chatState(),
                    onIntent = {},
                    onChatIntent = {},
                    onBack = {},
                    modifier = Modifier.requiredSize(WINDOW_WIDTH, WINDOW_HEIGHT),
                )
            }
        }
        compose.waitForIdle()
    }

    /** Scrolls the article down and returns where it landed, having checked that it moved. */
    private fun scrollTheStory(): Float {
        storyPane().performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, SCROLL_BY_PX) }
        compose.waitForIdle()

        val offset = storyPane().scrollOffset()
        assertTrue(
            "the story did not scroll, so both assertions below would hold for a pane that had " +
                "been reset to the top as well. Give DISCOVERY more sections.",
            offset > 0f,
        )
        return offset
    }

    private fun storyPane() =
        compose.onAllNodes(hasScrollAction()).filterToOne(startsAtTheLeadingEdge)

    private fun message(id: String, role: ChatRole) = ChatMessage(
        id = id,
        discoveryId = DISCOVERY.id,
        role = role,
        content = "The temple was founded in 1070 and is the country's first university.",
        createdAt = Instant.fromEpochSeconds(0),
    )

    private companion object {
        /** Far enough to be unmistakably past the first screenful of the article. */
        const val SCROLL_BY_PX = 900f

        /** A pixel offset compared against itself; half a pixel is rounding, not movement. */
        const val TOLERANCE = 0.5f

        /**
         * Long enough that the story pane genuinely scrolls at 800 dp tall.
         *
         * Eight sections of real prose rather than one placeholder repeated, because
         * `StoryBody` lays each one out as a titled block and a shorter article would leave the
         * pane unscrollable — at which point every assertion in this class would hold trivially.
         */
        val DISCOVERY = Discovery(
            id = "d1",
            title = "Temple of Literature",
            localName = "Văn Miếu",
            category = DiscoveryCategory.LANDMARK,
            imagePath = null,
            summary = "Vietnam's first university, founded in 1070 and dedicated to Confucius.",
            sections = List(8) { index ->
                DiscoverySection(
                    title = "Section $index",
                    body = "The courtyards run north in a line, each one a gate further from " +
                        "the street and a little quieter than the last. Stone stelae rest on " +
                        "turtles in the third, carrying the names of the doctors who passed " +
                        "the royal examinations here over three hundred years.",
                )
            },
            funFacts = listOf("The turtles are carved from blue stone quarried in Thanh Hóa."),
            tags = listOf("history", "architecture"),
            nearbySuggestions = emptyList(),
            suggestedQuestions = listOf("Who was Chu Văn An?"),
            confidence = 0.92f,
            location = GeoPoint(21.0278, 105.8355),
            placeHint = "Hanoi",
            isFavorite = false,
            modelUsed = null,
            createdAt = Instant.fromEpochSeconds(0),
        )
    }
}
