package com.evora.technologies.saola.feature.collection

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.evora.technologies.saola.core.designsystem.theme.SaolaTheme
import com.evora.technologies.saola.domain.model.CollectionSection
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.feature.collection.component.CollectionViewToggle
import com.evora.technologies.saola.feature.collection.component.collectionBoard
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_show_guide
import com.evora.technologies.saola.testing.collectionEntry
import com.evora.technologies.saola.testing.discovery
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The recognition hints are on the board, not behind sixty-one taps.
 *
 * The catalogue has carried a hint for every entry since it was written — `CatalogItem.hint`
 * calls it "the part that makes this a guide rather than a checklist" — and until the guide
 * mode existed the only way to read one was to tap a tile and open a sheet. A traveller
 * standing on a street they have never walked down does not do that sixty-one times, so the
 * board was a record of what they had photographed and never the guide it was written to be.
 *
 * **What is asserted is the switch, because that is the part that can regress silently.** The
 * hint text either is or is not on screen; a change that quietly drops the mode — a branch
 * inverted, the flag not threaded from `CollectionState` into one of the two arrangements —
 * leaves a board that still looks completely correct.
 *
 * The board is emitted into a plain `LazyColumn` here rather than driven through
 * `CollectionScreen`: `collectionBoard` is a `LazyListScope` extension precisely so the two
 * arrangements can put it in two different scrollers, and the thing under test is the
 * extension, not either page around it.
 *
 * **A guide row is asserted by its description, not by its text.** The row speaks once — name
 * then hint, through `clearAndSetSemantics` — because merged on its own it would read the
 * face's own description as well and announce the name twice on every one of sixty-one rows.
 * That is deliberate, so a text matcher would be testing against a bug rather than the design.
 */
class CollectionGuideTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun theBoardShowsNamesAndTheGuideShowsHowToRecogniseThem() {
        var showGuide = ""

        compose.setContent {
            showGuide = stringResource(Res.string.collection_show_guide)
            var isGuide by remember { mutableStateOf(false) }
            SaolaTheme {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        CollectionViewToggle(isGuide = isGuide, onToggle = { isGuide = !isGuide })
                    }
                    collectionBoard(
                        sections = SECTIONS,
                        columns = 3,
                        isGuide = isGuide,
                        onOpenDiscovery = {},
                        onShowHint = {},
                    )
                }
            }
        }

        // The board: the name is under the square, and the hint is nowhere at all.
        compose.onNodeWithText(PHO_NAME).assertIsDisplayed()
        compose.onNodeWithContentDescription(spoken(PHO_NAME, PHO_HINT)).assertDoesNotExist()

        compose.onNodeWithContentDescription(showGuide).performClick()
        compose.waitForIdle()

        compose.onNodeWithContentDescription(spoken(PHO_NAME, PHO_HINT)).assertIsDisplayed()
    }

    @Test
    fun aGuideRowIsOneAffordanceAndItLeadsWhereTheTileWouldHave() {
        var openedDiscovery: String? = null
        var hintedItem: String? = null

        compose.setContent {
            SaolaTheme {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    collectionBoard(
                        sections = SECTIONS,
                        columns = 3,
                        isGuide = true,
                        onOpenDiscovery = { openedDiscovery = it },
                        onShowHint = { hintedItem = it },
                    )
                }
            }
        }

        // Collected: the traveller already knows what it is and wants their own page back.
        compose.onNodeWithContentDescription(spoken(PHO_NAME, PHO_HINT)).performClick()
        compose.waitForIdle()
        assertEquals("d-pho", openedDiscovery)

        // Uncollected: nothing of theirs to open, so it offers the camera instead. The row is
        // the only click region — the face inside it deliberately has none, or one row would
        // answer two ways depending on where in it the finger landed.
        compose.onNodeWithContentDescription(spoken(BANH_MI_NAME, BANH_MI_HINT)).performClick()
        compose.waitForIdle()
        assertEquals("banh-mi", hintedItem)
    }
}

/** What a guide row says out loud, as `CollectionGuideRow` composes it. */
private fun spoken(name: String, hint: String) = "$name. $hint"

private val SECTIONS = listOf(
    CollectionSection(
        category = DiscoveryCategory.FOOD,
        entries = listOf(
            collectionEntry(
                id = "pho",
                name = PHO_NAME,
                hint = PHO_HINT,
                discovery = discovery(id = "d-pho"),
            ),
            collectionEntry(id = "banh-mi", name = BANH_MI_NAME, hint = BANH_MI_HINT),
        ),
    ),
)

private const val PHO_NAME = "Phở"
private const val PHO_HINT = "Flat ivory rice noodles in a clear bone broth"
private const val BANH_MI_NAME = "Bánh mì"
private const val BANH_MI_HINT = "A short baguette split and packed with pork and herbs"
