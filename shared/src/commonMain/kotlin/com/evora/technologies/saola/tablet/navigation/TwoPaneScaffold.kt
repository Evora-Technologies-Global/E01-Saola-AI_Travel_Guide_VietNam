package com.evora.technologies.saola.tablet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth

/**
 * Two panes side by side: one that keeps a fixed width, one that takes what is left.
 *
 * The shape every large-window screen in this app turns out to be. A viewfinder with its
 * controls beside it, a story with the guide next to it, a list of days with the day open
 * next to it — each is one pane whose width was measured and one that simply fills.
 *
 * It is deliberately the *only* file that reads [PaneWidth.lensPanel], [PaneWidth.guide] and
 * [PaneWidth.journalList] through a caller: a screen names the width it wants and hands it
 * over, so changing the proportions is one file's work rather than five screens'.
 *
 * It draws a `Row` and a divider and nothing else — no padding, no background, no header.
 * Anything a screen wants around its content is that screen's business, and it is what keeps
 * this from turning into a second `Scaffold`.
 *
 * **Four parameters, and it stays four.** A fifth is the signal that the screen asking for it
 * should be building its own `Row` — that is how a shared layout helper turns into a widget
 * with a mode for every caller, and by then no one can change it without checking five
 * screens. `modifier` is not one of the four; it is the Compose contract for any composable
 * that draws, and it carries no decision of its own.
 *
 * @param fixedPaneWidth from [PaneWidth]. Never a literal — the widths are measured positions
 *   read off the tablet wireframe, and one typed at a call site is a proportion that stopped
 *   agreeing with the four next to it.
 * @param fixedPaneAtStart which side the measured pane sits on. The journal is the reason
 *   this exists: its day column is the master half and reads left-to-right into the detail,
 *   while the lens and the discovery both put their fixed pane on the end.
 * @param fixedPane the measured pane — the panel, the guide column, the list of days.
 * @param flexiblePane the pane that takes the rest, and the one the traveller is looking at.
 */
@Composable
fun TwoPaneScaffold(
    fixedPaneWidth: Dp,
    fixedPaneAtStart: Boolean,
    fixedPane: @Composable () -> Unit,
    flexiblePane: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        if (fixedPaneAtStart) {
            FixedPane(width = fixedPaneWidth, content = fixedPane)
            PaneDivider()
        }

        // `weight` before `fillMaxSize` and no `minWidth`: the flexible pane is measured from
        // what the fixed one leaves, so a screen that overflows it has been given a fixed pane
        // too wide for the window rather than a bug to work around here.
        Box(modifier = Modifier.weight(1f).fillMaxSize()) { flexiblePane() }

        if (!fixedPaneAtStart) {
            PaneDivider()
            FixedPane(width = fixedPaneWidth, content = fixedPane)
        }
    }
}

@Composable
private fun FixedPane(width: Dp, content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(width).fillMaxHeight()) { content() }
}

/**
 * A hairline between the panes, in the scheme's outline colour.
 *
 * Two panes meeting on a shared background read as one pane with an odd gap in it. The
 * divider is what says the story stops and the guide starts, and it is a `VerticalDivider`
 * rather than a drawn line so it takes its colour and thickness from the theme.
 */
@Composable
private fun PaneDivider() {
    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
