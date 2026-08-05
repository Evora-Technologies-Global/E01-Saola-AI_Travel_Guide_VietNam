package com.evora.technologies.saola.core.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much room the window has — which is not the same question as what device it is.
 *
 * [COMPACT] gets the phone arrangement (`mobile/`), [EXPANDED] the large-window one
 * (`tablet/`). There is deliberately no MEDIUM: the wireframe was drawn at one size,
 * 1194 × 834, and a third arrangement nobody has designed would be three arrangements
 * to keep in step instead of two.
 */
enum class WindowClass { COMPACT, EXPANDED }

/**
 * The width at which the rail and the two panes start to fit.
 *
 * Below it the tablet shell would be drawing a 104dp rail into a quarter of the screen —
 * an unfolded fold is ~673dp, and split-screen on a tablet is routinely narrower still.
 */
private val ExpandedMinWidth = 840.dp

/**
 * And the height, which matters just as much and is the condition that is easy to forget.
 *
 * A phone held sideways is ~891dp wide — wider than the threshold above — and ~411dp tall.
 * On width alone it would be handed the master–detail layout, whose master column is 392dp
 * and whose detail pane assumes a page's worth of vertical room. Both conditions, or a
 * phone in landscape breaks in a way that only shows up on a real device.
 */
private val ExpandedMinHeight = 600.dp

/**
 * Classifies the window the app is actually in, measured rather than guessed.
 *
 * Fed from `BoxWithConstraints` at the root of the composition rather than from
 * `material3-window-size-class`: that library is Android-only, while this file has to
 * answer the same question on iOS. Measuring also reports the truth in split-screen,
 * where the app owns a fraction of a display it could otherwise ask about and be misled by.
 */
@Composable
fun rememberWindowClass(maxWidth: Dp, maxHeight: Dp): WindowClass =
    remember(maxWidth, maxHeight) { windowClassOf(maxWidth, maxHeight) }

/**
 * The classification itself, with no composition around it.
 *
 * Split out so `WindowClassTest` can walk the boundaries. Both thresholds are inclusive and
 * both are checked at exactly their own value — an iPad in portrait is 834pt and misses
 * [ExpandedMinWidth] by six, which is close enough that flipping one `>=` to `>` would change
 * a real device's branch and no screenshot would look wrong enough to notice.
 */
internal fun windowClassOf(maxWidth: Dp, maxHeight: Dp): WindowClass =
    if (maxWidth >= ExpandedMinWidth && maxHeight >= ExpandedMinHeight) {
        WindowClass.EXPANDED
    } else {
        WindowClass.COMPACT
    }

/**
 * The height below which a phone screen can no longer stack its parts down the page.
 *
 * A **position, not a gap**, so it is named and measured rather than taken off the
 * [com.evora.technologies.saola.core.designsystem.theme.Spacing] scale — `LLM.md` §13.2.
 * What it is measured against is the lens, because the lens is the screen that fails
 * first and by the widest margin: its column is a 44dp tool row inside `Spacing.sm`
 * either side (60), a chip row inside `Spacing.md` either side (68), and a 78dp shutter
 * with `Spacing.sm` under it (86) — **214dp of chrome before the picture gets a pixel**.
 * Leave the frame 280dp, which on a 352dp-wide phone is roughly 4:5 and the shallowest
 * rectangle that still reads as a viewfinder rather than a slot, and the column needs
 * 494. Rounded to 500.
 *
 * Deliberately *not* [ExpandedMinHeight]. That 600 is the height a **two-pane** layout
 * needs and it answers a different question; the two agreeing to within 100dp today is a
 * coincidence, and wiring them together would mean a change to the tablet's gate silently
 * re-laying-out the phone.
 */
private val StackableMinHeight = 500.dp

/**
 * Whether this window has the height to stack — the second question, asked on its own axis.
 *
 * [rememberWindowClass] chooses the *branch*; this chooses an *arrangement inside* one, and
 * both COMPACT answers are `mobile/`. A phone held sideways is ~832 × 384dp on the Galaxy
 * A16 this was measured on: too narrow for the tablet branch, and too short for the phone's
 * own column. It is a question about height alone, so a short freeform or split-screen
 * window gets the same answer as a rotated phone — which is correct, because what fails is
 * the stacking and not the rotation.
 *
 * Only the lens reads this today. Every other phone screen either scrolls or fits: verified
 * screen by screen on the A16 in landscape on 05.08.2026, and the two that neither scroll nor
 * stack survive for reasons of their own — `VietnamMapCanvas` fits Vietnam to
 * `min(width/worldWidth, height/worldHeight)` so a short window simply draws it smaller, and
 * the chat composer takes `imePadding()` under `adjustResize`.
 */
@Composable
fun rememberCanStackVertically(maxHeight: Dp): Boolean =
    remember(maxHeight) { canStackVertically(maxHeight) }

/** [rememberCanStackVertically] with no composition around it, so the boundary is testable. */
internal fun canStackVertically(maxHeight: Dp): Boolean = maxHeight >= StackableMinHeight
