package com.evora.technologies.saola.feature.passport.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.MapSourceNote
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.feature.passport.PassportState
import com.evora.technologies.saola.feature.passport.VietnamMapCanvas
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.map_source_osm
import org.jetbrains.compose.resources.stringResource

/**
 * The map in a framed card.
 *
 * The card is a container only — the drawing inside it, its colours and its gestures are
 * untouched. What it buys is a boundary: pinch-zoomed, the country now clearly lives inside a
 * panel instead of running loose between the heading and the banner.
 *
 * [VietnamMapCanvas] is used unchanged by both branches and stays where it is: it draws on a
 * `Canvas` and so already fits whatever it is given, and it is one of the three files
 * `DesignTokenTest` allows to write a type size — an allowlist keyed on the file name, which
 * moving the file would quietly empty.
 *
 * **The source credit is in this file rather than in either arrangement, and that is the
 * point of it being here.** Every outline the canvas traces is derived from OpenStreetMap's
 * boundary relations, so ODbL asks for the notice wherever the result is drawn — and both the
 * phone's screen and the large window's pane draw exactly this. Put in the two arrangements
 * instead, it would be worded twice and dropped from the next one.
 */
@Composable
internal fun PassportMap(
    state: PassportState,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(FRAME, PassportHairline, shape)
            .padding(Spacing.xxs),
    ) {
        VietnamMapCanvas(
            stamps = state.passport.stamps,
            covers = state.covers,
            selectedProvinceId = state.selectedProvinceId,
            onSelect = onSelect,
            lockedFill = MaterialTheme.colorScheme.surfaceContainerHighest,
            // Not `outlineVariant`: against `surfaceContainerHighest` in
            // this app's own light scheme that measures 1.03:1, so the
            // first-launch map — 34 locked provinces and no photos yet —
            // renders as a single grey silhouette with no internal
            // borders at all, which is exactly the moment the 34 pieces
            // need to be legible.
            lockedStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = LOCKED_STROKE_ALPHA),
            unlockedStroke = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNLOCKED_STROKE_ALPHA),
            insetLabel = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedStroke = Vermilion,
            modifier = Modifier.fillMaxSize(),
        )

        // Bottom-left, which on this projection is open sea in every window this card is
        // given: Vietnam is far taller than it is wide, so the aspect-fit always leaves that
        // corner empty. It is inside the frame rather than under it because the credit is
        // about the drawing, and a line under the card reads as a caption for the page.
        MapSourceNote(
            text = stringResource(Res.string.map_source_osm),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.xs),
        )
    }
}

/** A hairline, in the only unit a border can be given one. */
private val FRAME = 1.dp

private const val LOCKED_STROKE_ALPHA = 0.45f
private const val UNLOCKED_STROKE_ALPHA = 0.75f
