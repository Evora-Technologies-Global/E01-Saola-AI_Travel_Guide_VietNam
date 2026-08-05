package com.duylt.trave.vietlensai.tablet

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.unit.dp

/**
 * The shared machinery of the two two-pane scroll tests.
 *
 * Both ask the same question of a different screen — *did the pane nobody touched stay where
 * it was?* — and both start the same way: find the scrollable filling one pane, note where it
 * is, do the thing, look again. What they share is [startsAtTheLeadingEdge] and the forced
 * window; how they measure differs, and deliberately so — see [scrollOffset].
 *
 * Neither reads a `LazyListState` or a `ScrollState`. Both screens hold their scroll state
 * privately, which is correct, and a test that needed it hoisted into a parameter would have
 * changed the very arrangement it is checking.
 */

/**
 * The window both tests compose into, forced rather than taken from the device.
 *
 * These screens are only ever reached above 840 × 600 dp — `core/window/WindowClass.kt` sends
 * anything smaller to `mobile/` — but an instrumented test runs at whatever the AVD happens to
 * be. Left to the device, the same test would exercise two panes on one machine and a 392 dp
 * column with nothing beside it on another, and the second run would pass while proving
 * nothing. A fixed window is what makes a green tick mean the same thing everywhere.
 */
internal val WINDOW_WIDTH = 1280.dp
internal val WINDOW_HEIGHT = 800.dp

/**
 * The scrollable that starts at the window's leading edge.
 *
 * Both screens put the pane under test on the left — the journal's day column is its fixed
 * pane at the start, the discovery's story is the flexible pane with the guide on the trailing
 * edge — so `left == 0` identifies it in both, and neither screen needs a test tag.
 *
 * **One clause, and it is measured rather than reasoned.** On the Pixel Tablet at
 * 1280 × 800 dp the discovery reports exactly two scrollables — the story at
 * `(0, 192, 1854, 1600)` and the guide's thread at `(1856, 222, 2560, 1440)` — and the journal
 * exactly one, the day column at `(0, 48, 784, 1600)`, which is 392 dp at that density. Two
 * plausible-sounding extra clauses were tried against those numbers and both were wrong:
 * `top == 0` fails because `screenInsetsPadding()` moves every pane down by the cutout, by a
 * different amount on each screen; "taller than it is wide" fails because the story pane is
 * 1280 less the guide and the divider, wider than the window is tall.
 *
 * `filterToOne` is the guard against this being too loose. If a second scrollable ever lands on
 * the leading edge the test fails naming both, which is the right outcome — an arrangement that
 * has grown a second scroller at the window's edge is a change these tests should be re-read
 * against rather than silently applied to whichever one came first.
 */
internal val startsAtTheLeadingEdge =
    SemanticsMatcher("is the scrollable starting at the window's leading edge") { node ->
        node.boundsInRoot.left < EDGE_TOLERANCE_PX
    }

/** A pane sits *on* the edge; anything inside one is at least a gutter in, which is far more. */
private const val EDGE_TOLERANCE_PX = 1f

/**
 * How far the pane has been scrolled, as Compose reports it to accessibility.
 *
 * **Used by the discovery test only, and the reason the journal's does not is worth stating.**
 * The story pane is a `Modifier.verticalScroll` column, so this is a straight pixel offset and
 * two readings of it are directly comparable. A `LazyColumn` reports something else — an
 * estimate built from the first visible index and its offset — and on the journal's column a
 * short scroll that never leaves item 0 moves that estimate by about a thousandth, which no
 * honest tolerance can tell from a list reset to the top. The journal therefore measures the
 * top edge of a row it can see instead, in pixels, which is also the thing the traveller
 * actually notices moving.
 */
internal fun SemanticsNodeInteraction.scrollOffset(): Float =
    fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
