package com.evora.technologies.saola.feature.passport.component

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/**
 * One province, as a screen reader needs it: where it is, what it is called, what it costs to
 * open it.
 *
 * @param bounds in the canvas's own unmagnified space — the same space the paths are built in.
 *   The transform is applied at measure time rather than baked in here, so a pinch does not
 *   rebuild thirty-six of these.
 * @param isFixed true for the two archipelago insets. They are a separate reference frame that
 *   deliberately does not pan or zoom with the mainland, exactly as they are drawn.
 */
internal data class ProvinceHandle(
    val id: String,
    val label: String,
    val bounds: Rect,
    val isFixed: Boolean = false,
)

/** Where the mainland currently sits, read at measure time. */
internal data class MapPlacement(val zoom: Float, val offset: Offset)

/**
 * The map, told to a screen reader.
 *
 * **Before this, the passport did not exist for anyone using TalkBack or VoiceOver.** The map is
 * a `Canvas` with two `pointerInput`s and nothing else — thirty-four `Path`s stroked into one
 * node with no label, no children and no actions — and the screen around it is a heading, a
 * progress bar and a banner. There was no list to fall back to and no way to name a province,
 * let alone open one: the whole feature was a blank rectangle that announced nothing.
 *
 * **Semantics only, with no pointer input of its own, and that is what makes it free.** These
 * boxes draw nothing and consume nothing: a `Box` with no `clickable` does not take pointer
 * events, so pan, pinch and tap still reach the canvas underneath exactly as before. A screen
 * reader's double-tap does not synthesise a touch either — it invokes the `onClick` semantics
 * action directly — so the same gesture that works for a sighted traveller and the one that
 * works for a listening one are two different mechanisms over one map, and neither is in the
 * other's way. Making these `clickable` instead would have handed every tap to whichever
 * bounding box happened to be on top, which for two provinces that interlock is not the one
 * under the finger.
 *
 * **Bounding boxes overlap, and the largest is placed first so the smallest stays reachable.**
 * A province is a polygon; its box is not. Hà Nội's box sits entirely inside the box around
 * Phú Thọ, and a node a later sibling completely covers is reported as not visible — so in
 * asset order those two provinces were dumped by `uiautomator` as absent from the tree
 * altogether, which for a screen reader means unreachable. Sorting by descending area removes
 * the case by construction: a later sibling is never larger, and a smaller rectangle cannot
 * swallow a bigger one. Measured on a Galaxy A16 on 06.08.2026 — 32 of 34 provinces in the
 * tree before, 34 after.
 *
 * That leaves the focus rectangle approximate near an interlocking border, which is accepted:
 * hit-testing a real outline is not something the semantics tree can express, and an
 * approximate rectangle with the right name and a working action is worth incomparably more
 * than an exact silence.
 *
 * **Reading order is restored separately.** Area order is a rendering constraint and nothing a
 * traveller should hear, so each node carries a `traversalIndex` taken from its position in
 * [handles] — the order the asset lists them, which is alphabetical — and the group is marked
 * a traversal group so those indices are compared against each other and not against the rest
 * of the page.
 *
 * @param placement read inside the measure policy, not during composition. That is the whole
 *   reason this is a `Layout`: a pinch writes `zoom` sixty times a second, and reading it in a
 *   composable would re-run thirty-six compositions per frame to move boxes that draw nothing.
 *   Read here it invalidates measurement alone.
 */
@Composable
internal fun ProvinceSemanticsOverlay(
    handles: List<ProvinceHandle>,
    placement: () -> MapPlacement,
    openLabel: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ordered = remember(handles) {
        handles.withIndex().sortedByDescending { (_, handle) ->
            handle.bounds.width * handle.bounds.height
        }
    }

    Layout(
        modifier = modifier.semantics { isTraversalGroup = true },
        content = {
            ordered.forEach { (readingOrder, handle) ->
                Box(
                    Modifier.semantics {
                        contentDescription = handle.label
                        role = Role.Button
                        traversalIndex = readingOrder.toFloat()
                        onClick(label = openLabel) {
                            onSelect(handle.id)
                            true
                        }
                    },
                )
            }
        },
    ) { measurables, constraints ->
        val (zoom, offset) = placement()

        val placed = measurables.mapIndexed { index, measurable ->
            val handle = ordered.getOrNull(index)?.value
            val rect = when {
                handle == null -> Rect.Zero
                handle.isFixed -> handle.bounds
                else -> Rect(
                    left = handle.bounds.left * zoom + offset.x,
                    top = handle.bounds.top * zoom + offset.y,
                    right = handle.bounds.right * zoom + offset.x,
                    bottom = handle.bounds.bottom * zoom + offset.y,
                )
            }
            // Clamped to the viewport so a province panned off the edge reports a node the
            // platform can dismiss as off-screen, rather than one measured at a negative
            // width — which Compose rejects outright.
            val width = rect.width.roundToInt().coerceIn(0, constraints.maxWidth)
            val height = rect.height.roundToInt().coerceIn(0, constraints.maxHeight)
            measurable.measure(Constraints.fixed(width, height)) to rect
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placed.forEach { (placeable, rect) ->
                placeable.place(rect.left.roundToInt(), rect.top.roundToInt())
            }
        }
    }
}
