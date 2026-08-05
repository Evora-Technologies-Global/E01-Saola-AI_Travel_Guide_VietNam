package com.duylt.trave.vietlensai.tablet.feature.sovereignty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.PinSystemBarIcons
import com.duylt.trave.vietlensai.core.designsystem.component.lacquerHatch
import com.duylt.trave.vietlensai.core.designsystem.theme.PaneWidth
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding
import com.duylt.trave.vietlensai.feature.sovereignty.RegionMap
import com.duylt.trave.vietlensai.feature.sovereignty.SovereigntyViewModel
import com.duylt.trave.vietlensai.feature.sovereignty.component.SovereigntyCloseButton
import com.duylt.trave.vietlensai.feature.sovereignty.component.SovereigntyHeading
import com.duylt.trave.vietlensai.feature.sovereignty.component.SovereigntyMapPanel
import com.duylt.trave.vietlensai.feature.sovereignty.component.SovereigntyStatement
import com.duylt.trave.vietlensai.feature.sovereignty.component.SovereigntyUnderstoodButton
import org.koin.compose.viewmodel.koinViewModel

/**
 * The statement as a sheet laid over the window: what is claimed on the left, and the water it
 * is claimed in on the right.
 *
 * The wireframe draws it `position:absolute` across the whole frame with a `×` at the top left,
 * rather than as another page pushed onto the stack. It **is** still a destination —
 * `Routes.SOVEREIGNTY`, registered in both shells, because the two graphs have to compare
 * structurally equal or `setGraph` clears the back stack on every resize (`LLM.md` §7) and
 * because the seal at the foot of the rail and the row in Settings both navigate to it. What
 * makes it read as an overlay is that no rail is drawn beside it — `railDestination()` returns
 * null here, so the red reaches all four edges of the glass.
 *
 * **The map is a pane, not a figure in the prose.** This is the one place the two branches put
 * the statement's parts in a genuinely different order, and the reason is that a large window
 * does not have to choose one. A phone reads down: the claim, then the map that shows what it
 * is about, then the evidence — prose first there would leave the traveller reading about two
 * archipelagos they cannot place. Given 1 194 dp the two can be read *at once*, so the map
 * stops interrupting the sentence and becomes the thing the sentence is pointing at.
 *
 * That is also what sets the two widths. The document is held to [PaneWidth.sheet] — the app's
 * one measured width for something that floats over an arrangement rather than covering it —
 * because at full width the same prose run would be a 70-word line, and the eye loses its place
 * returning from the end of one to the start of the next. Everything left over is the map's,
 * which is how the figure ends up the larger of the two: the claim is a paragraph, and what it
 * is about is a sea.
 *
 * The note panel the phone prints under the map is not drawn here. It says where the statement
 * appears, which is a footnote to a page — and this arrangement has no page foot, only two
 * columns that end at different heights.
 */
@Composable
fun SovereigntyTabletRoute(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SovereigntyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SovereigntyOverlay(map = state.map, onClose = onClose, modifier = modifier)
}

@Composable
private fun SovereigntyOverlay(
    map: RegionMap?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deep red under the bars in either theme, so the icons have to be light in both.
    PinSystemBarIcons(darkIcons = false)

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Vermilion)
            .lacquerHatch()
            // The red still runs under the cutout and only the content stops short of it. On a
            // tablet held in landscape that hole is on a side edge, which is why this is
            // `screenInsetsPadding()` and not a top-only inset.
            .screenInsetsPadding()
            .navigationBarsPadding()
            .padding(ScreenGutter),
    ) {
        StatementColumn(
            onClose = onClose,
            modifier = Modifier.width(PaneWidth.sheet).fillMaxHeight(),
        )

        Spacer(Modifier.width(Spacing.xxl))

        // Whatever is left of the window, floor to ceiling. The drawing is fitted inside and
        // centred, so a window that is taller than the map is wide shows more of the panel's
        // own washed water above and below it rather than a band of bare page.
        SovereigntyMapPanel(
            map = map,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

/**
 * The document, and the way out held above it.
 *
 * The close button is outside the scroll rather than at the head of it — the one thing this
 * arrangement changes about the phone's. There the document is the page, so a `×` that stayed
 * put would sit on top of the prose it is meant to leave; here it has a column of its own to
 * stand at the top of, and it keeps its position while the statement travels under it.
 *
 * The statement scrolls even though it fits: it fits *in English on a 800 dp-high window*, and
 * the eight languages this app ships in do not agree about how many lines a paragraph is, nor
 * does the traveller's font scale.
 */
@Composable
private fun StatementColumn(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        SovereigntyCloseButton(onClick = onClose)

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(Spacing.xl))
            SovereigntyHeading()

            Spacer(Modifier.height(Spacing.xl))
            SovereigntyStatement()

            Spacer(Modifier.height(Spacing.xl))
            SovereigntyUnderstoodButton(onClick = onClose)
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
