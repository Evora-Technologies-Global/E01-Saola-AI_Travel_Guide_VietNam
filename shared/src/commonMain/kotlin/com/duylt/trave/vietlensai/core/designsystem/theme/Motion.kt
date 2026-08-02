package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * How long things take to arrive and to leave, and along what curve.
 *
 * One set of numbers for the whole app, for the same reason [ScreenGutter] is one number:
 * the traveller crosses three screens in ten seconds, and a card that fades in over 150ms
 * on one screen and 400ms on the next reads as a stutter rather than as two decisions.
 *
 * Arriving and leaving are deliberately not symmetrical. Something coming in is being
 * introduced — it decelerates into place and the eye is meant to follow it. Something going
 * out has already been decided about, and holding it on screen for as long as it took to
 * appear only delays whatever the traveller pressed for.
 */
object Motion {

    /** A badge, a chip, a control changing state where it stands. */
    const val QUICK_MILLIS = 160

    /** The default: a card, a block, a piece of a page appearing or leaving. */
    const val STANDARD_MILLIS = 260

    /** Whole-page changes, where the eye has to travel before it can read anything. */
    const val EMPHASISED_MILLIS = 340

    /** Almost all deceleration: fast off the mark, long settle. */
    private val EnterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** The mirror of it — slow to let go, then gone. */
    private val ExitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    fun <T> enter(durationMillis: Int = STANDARD_MILLIS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = EnterEasing)

    fun <T> exit(durationMillis: Int = QUICK_MILLIS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = ExitEasing)

    /**
     * For a container resizing around content, or content sliding to make room for it.
     *
     * Eased at both ends rather than only on arrival: nothing is entering or leaving here,
     * something already on screen is being moved, and a curve that starts at full speed
     * reads as the layout snapping.
     */
    fun <T> morph(durationMillis: Int = STANDARD_MILLIS): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
}
