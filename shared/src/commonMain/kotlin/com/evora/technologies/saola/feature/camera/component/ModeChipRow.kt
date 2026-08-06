package com.evora.technologies.saola.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_mode_selector
import org.jetbrains.compose.resources.stringResource

/**
 * The mode picker, as a row of chips.
 *
 * It was a dropdown in the top-left corner, which is the one part of a phone a hand
 * holding it cannot reach — and it hid four of the five modes behind a tap, so a
 * traveller who never opened it never learned the app could read a menu. As chips
 * the whole set is legible at rest and every one of them is under the thumb that is
 * about to press the shutter. Scrollable because a longer translation of five
 * labels can outrun a narrow screen.
 *
 * The phone puts it between the frame and the shutter; the tablet puts it at the head of
 * the viewfinder pane, beside the camera tools. Both draw the same chips — the row takes a
 * `modifier` and nothing else, so where it lands is the arrangement's decision and how it
 * looks is not.
 */
@Composable
internal fun ModeChipRow(
    modes: List<LensMode>,
    selected: LensMode,
    onSelect: (LensMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.camera_mode_selector)
    val listState = rememberLazyListState()

    // Five labels of unknown length in whatever language the phone is set to will
    // eventually outrun a narrow screen, so the row scrolls — and then has to make
    // sure the chosen mode is not the one off the edge. This matters on the way *in*
    // rather than on a tap: coming back to the tab restores a mode the traveller
    // picked ages ago, and a screen that opens claiming "Auto" while it is set to
    // translate is worse than one that scrolls.
    LaunchedEffect(selected, modes) {
        val index = modes.indexOf(selected)
        if (index < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val chip = info.visibleItemsInfo.firstOrNull { it.index == index }
        val whollyVisible = chip != null &&
            chip.offset >= info.viewportStartOffset &&
            chip.offset + chip.size <= info.viewportEndOffset
        if (!whollyVisible) listState.animateScrollToItem(index)
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(modes) { mode ->
            val active = mode == selected
            Text(
                text = stringResource(mode.labelRes()),
                // A step under the app's label size, which is set for reading
                // paragraphs: five chips have to share one line, and this is a row of
                // switches rather than something anybody reads. Selection is carried by
                // the gold fill and the ink on it, not by a second weight — a chip that
                // is both filled and bolder is saying the same thing twice.
                style = MaterialTheme.typography.labelMedium,
                color = if (active) InkBrown else PaperCream,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .clip(Pill)
                    .background(if (active) Marigold else PaperCream.copy(alpha = 0.12f))
                    .clickable(
                        role = Role.RadioButton,
                        onClickLabel = label,
                        onClick = { onSelect(mode) },
                    )
                    // Spoken as well as coloured: the chip row is a single choice, and
                    // TalkBack has no way to hear a gold background.
                    .semantics { this.selected = active }
                    // 12, not 8: this row is the primary mode selector and the type scale
                    // already dropped a step, so the smaller pad would take the chip from
                    // 40 dp to 32 — shrinking the target while standardising it.
                    .padding(horizontal = Spacing.md, vertical = Spacing.md),
            )
        }
    }
}
