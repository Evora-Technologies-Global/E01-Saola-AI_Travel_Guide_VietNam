package com.duylt.trave.vietlensai.core.window

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
    remember(maxWidth, maxHeight) {
        if (maxWidth >= ExpandedMinWidth && maxHeight >= ExpandedMinHeight) {
            WindowClass.EXPANDED
        } else {
            WindowClass.COMPACT
        }
    }
