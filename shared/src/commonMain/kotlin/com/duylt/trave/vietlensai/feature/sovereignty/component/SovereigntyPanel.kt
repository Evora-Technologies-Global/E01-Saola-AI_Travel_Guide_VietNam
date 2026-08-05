package com.duylt.trave.vietlensai.feature.sovereignty.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntySeal
import com.duylt.trave.vietlensai.core.designsystem.component.lacquerHatch
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.feature.sovereignty.RegionMap
import com.duylt.trave.vietlensai.feature.sovereignty.SovereigntyMapCanvas
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.sovereignty_map_country
import com.duylt.trave.vietlensai.resources.sovereignty_map_sea
import com.duylt.trave.vietlensai.resources.sovereignty_note
import org.jetbrains.compose.resources.stringResource

// The statement's two panels — the figure and the note — in one file because they are one
// panel with two faces.
//
// Both are the page's own lacquer washed darker, hatched at the same low alpha and clipped to
// the same corner; the difference is that one holds a map and the other a seal and a sentence.
// They sit one above the other on the page with a single gap between them, so a change to
// either that misses the other is visible without scrolling — which is the test `LLM.md` §5
// sets for two composables sharing a file.
//
// Since 04.08.2026 only the phone draws the note: the large window gives the map a pane of its
// own and the note has no page left to sit at the foot of. It stays here rather than moving
// into `mobile/` because the wash, the corner and the hatch are the thing these two agree
// about, and a copy of that chain in a branch file is a second shade of the same panel — the
// argument §12 makes for `SovereigntyInk.kt`, one size down.

/**
 * The map in a panel of its own.
 *
 * **The caller gives it its size**, and the two branches disagree about it in the one way that
 * matters. The phone hands it [frameAspect], so the panel is exactly the shape of the frame and
 * keeps that height while the asset is still being read — the prose below it does not shuffle
 * down the moment the map arrives, and the drawing is undistorted. A large window hands it a
 * whole pane, and the map is drawn corner to corner of it: the frame is wider than it is tall
 * and the pane is not, so it is stretched to get there. `RegionProjection` states that trade and
 * what the two alternatives — cropping the sides, or re-cutting the frame — would have cost.
 */
@Composable
internal fun SovereigntyMapPanel(map: RegionMap?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .background(SeaWash)
            .lacquerHatch(alpha = SOVEREIGNTY_PANEL_HATCH_ALPHA),
    ) {
        if (map != null) {
            SovereigntyMapCanvas(
                map = map,
                countryLabel = stringResource(Res.string.sovereignty_map_country),
                seaLabel = stringResource(Res.string.sovereignty_map_sea),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** Where the statement is shown, stamped rather than stated. */
@Composable
internal fun SovereigntyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(SeaWash)
            .lacquerHatch(alpha = SOVEREIGNTY_PANEL_HATCH_ALPHA)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SovereigntySeal(size = SEAL_SIZE)
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = stringResource(Res.string.sovereignty_note),
            style = MaterialTheme.typography.bodyMedium,
            color = PaperCream.copy(alpha = SOVEREIGNTY_SECONDARY_ALPHA),
        )
    }
}

/**
 * The shape the map panel takes where it is sized to the drawing rather than to a pane.
 *
 * Null until the asset has been parsed, and [FRAME_ASPECT] is what `sovereignty_map.json`'s own
 * frame comes out at — so the panel is laid out at its final size on the first frame and the
 * page does not jump when the map lands.
 */
internal val RegionMap?.frameAspect: Float
    get() = this?.aspectRatio ?: FRAME_ASPECT

/** Matches the frame in `sovereignty_map.json`; only used before it has been read. */
private const val FRAME_ASPECT = 1.56f

/** Large enough to read the three words printed on the stamp, small enough to be a mark. */
private val SEAL_SIZE = 44.dp
