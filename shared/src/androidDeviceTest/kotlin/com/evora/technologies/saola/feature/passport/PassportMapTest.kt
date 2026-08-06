package com.evora.technologies.saola.feature.passport

import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.percentOffset
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.SaolaTheme
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.passport_map_a11y
import com.evora.technologies.saola.resources.passport_province_a11y
import com.evora.technologies.saola.resources.passport_status_locked
import com.evora.technologies.saola.resources.passport_status_unlocked
import com.evora.technologies.saola.testing.province
import com.evora.technologies.saola.testing.stamp
import org.jetbrains.compose.resources.stringResource
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * The passport map, checked on the two questions a `Canvas` cannot be reviewed for.
 *
 * `PassportViewModelTest` proves the state is right; nothing proved the drawing was, and the
 * drawing is where this feature lives. Six hundred lines of projection, transform and hit test
 * had no test on any platform — a sign error in `latitudeAt` or an off-by-one in `clamp` puts
 * a finger on Nghệ An and opens Thanh Hóa, and the only thing that would have caught it is
 * somebody noticing on a phone.
 *
 * **Two claims, and they are deliberately the two that can silently disagree.** A tap must
 * resolve to the province actually under it, and a screen reader must be offered the same
 * provinces by name with a working action. They are two different paths into one map —
 * `detectTapGestures` over the projection, and the semantics overlay over the same geometry —
 * and each is capable of being right while the other is wrong.
 *
 * **The geometry is four squares, not the shipped asset.** See `DeviceFixtures.province`.
 * Squares make the expected answers arithmetic rather than something re-derived from 9,151
 * real vertices every time the boundary data is corrected. Over a square extent the projection
 * fits exactly, so the mainland fills the map area and a quarter in from its top-left corner
 * is inside the north-west province at any density.
 *
 * **Run it on a device or an AVD at API ≤ 36.** `LLM.md` §11 row #18: on API 37
 * `Espresso.onIdle` dies in `InputManager.getInstance` before any test body executes.
 */
class PassportMapTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tappingAProvinceSelectsTheOneUnderTheFinger() {
        var selected: String? = "unset"
        var summary = ""

        compose.setContent {
            summary = mapSummary()
            SaolaTheme {
                MapUnderTest(stamps = STAMPS, onSelect = { selected = it })
            }
        }

        // A fifth of the way across and a quarter down: inside the mainland area, well clear
        // of the inset column that takes the right-hand quarter of the canvas.
        compose.onNodeWithContentDescription(summary)
            .performTouchInput { click(percentOffset(0.2f, 0.25f)) }
        compose.waitForIdle()
        assertEquals("nw", selected)

        // The same column, the lower half. Latitude grows north and screen y grows down, so
        // this is the one assertion that fails outright if that inversion is ever dropped.
        compose.onNodeWithContentDescription(summary)
            .performTouchInput { click(percentOffset(0.2f, 0.75f)) }
        compose.waitForIdle()
        assertEquals("sw", selected)
    }

    @Test
    fun everyProvinceIsOfferedToAScreenReaderWithItsState() {
        var visitedLabel = ""
        var notVisitedLabel = ""

        compose.setContent {
            visitedLabel = provinceLabel("nw", isVisited = true)
            notVisitedLabel = provinceLabel("se", isVisited = false)
            SaolaTheme {
                MapUnderTest(stamps = STAMPS, onSelect = {})
            }
        }

        // Before the overlay existed the whole map was one unlabelled node: someone using
        // TalkBack could not find out that a province existed, let alone which ones they had
        // been to.
        compose.onNodeWithContentDescription(visitedLabel).assertIsDisplayed()
        compose.onNodeWithContentDescription(notVisitedLabel).assertIsDisplayed()
    }

    @Test
    fun theMapItselfSaysWhatItIsAndHowFarAlongTheTravellerIs() {
        var summary = ""

        compose.setContent {
            summary = mapSummary()
            SaolaTheme {
                MapUnderTest(stamps = STAMPS, onSelect = {})
            }
        }

        compose.onNodeWithContentDescription(summary).assertIsDisplayed()
    }

    @Test
    fun aScreenReaderCanOpenAProvinceWithoutTouchingItsPixels() {
        var selected: String? = null
        var label = ""

        compose.setContent {
            label = provinceLabel("ne", isVisited = true)
            SaolaTheme {
                MapUnderTest(stamps = STAMPS, onSelect = { selected = it })
            }
        }

        // The semantics action, not a touch: `performClick` injects a real gesture, and
        // these nodes deliberately have no pointer input for one to land on. Invoking the
        // action is exactly what TalkBack's double-tap does, and that separation is the
        // design — the gestures below keep every touch.
        compose.onNodeWithContentDescription(label)
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()

        assertEquals("ne", selected)
    }
}

/** The sentence the canvas node carries: two of four squares are visited. */
@Composable
private fun mapSummary(): String = stringResource(Res.string.passport_map_a11y, 2, 4)

/**
 * One province's label, resolved the way the screen resolves it.
 *
 * Read out of the resource table rather than written as English: these run on whatever locale
 * the device happens to be in, and a hardcoded "Tỉnh nw, Đã đến" would pass on a Vietnamese
 * phone and fail on every other one.
 */
@Composable
private fun provinceLabel(id: String, isVisited: Boolean): String = stringResource(
    Res.string.passport_province_a11y,
    "Tỉnh $id",
    stringResource(
        if (isVisited) Res.string.passport_status_unlocked else Res.string.passport_status_locked,
    ),
)

/**
 * The canvas at a fixed size, with the colours the passport gives it.
 *
 * `requiredSize` rather than `fillMaxSize`: the assertions above are about where things are,
 * and a canvas the size of whatever device is running would make "a fifth of the way across"
 * mean a different province on a tablet.
 */
@Composable
private fun MapUnderTest(stamps: List<PassportStamp>, onSelect: (String?) -> Unit) {
    VietnamMapCanvas(
        stamps = stamps,
        covers = emptyMap(),
        selectedProvinceId = null,
        onSelect = onSelect,
        lockedFill = Color.LightGray,
        lockedStroke = Color.Gray,
        unlockedStroke = Color.DarkGray,
        selectedStroke = Color.Red,
        insetLabel = Color.Black,
        modifier = Modifier.requiredSize(CANVAS_SIDE),
    )
}

/**
 * Four equal squares over a 2° × 2° extent, two of them visited.
 *
 * Named by compass quadrant so an assertion reads as a position rather than as an id, and kept
 * in a fixed order because `handleGeometry` walks the stamps in the order it is given. Neither
 * square has an offshore ring, so both archipelago insets come out ownerless — drawn, but
 * contributing no semantics node, which is what `handleGeometry` promises.
 */
private val STAMPS = listOf(
    stamp(
        province("nw", minLongitude = 100.0, minLatitude = 11.0, maxLongitude = 101.0, maxLatitude = 12.0),
        discoveryCount = 3,
    ),
    stamp(
        province("ne", minLongitude = 101.0, minLatitude = 11.0, maxLongitude = 102.0, maxLatitude = 12.0),
        discoveryCount = 1,
    ),
    stamp(province("sw", minLongitude = 100.0, minLatitude = 10.0, maxLongitude = 101.0, maxLatitude = 11.0)),
    stamp(province("se", minLongitude = 101.0, minLatitude = 10.0, maxLongitude = 102.0, maxLatitude = 11.0)),
)

/** Square, so the four provinces come out square and the arithmetic above holds. */
private val CANVAS_SIDE = 400.dp
