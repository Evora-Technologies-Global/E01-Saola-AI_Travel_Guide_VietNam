package com.evora.technologies.saola.feature.translate

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipe
import com.evora.technologies.saola.core.designsystem.theme.SaolaTheme
import com.evora.technologies.saola.domain.model.TextBox
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationBlock
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.mobile.feature.translate.TranslationScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

/**
 * Pinch and drag on the result screen, driven as real multi-touch.
 *
 * Written as an instrumented test rather than an adb script because `adb shell
 * input` has no second finger and this phone denies the shell access to the raw
 * input device — a pinch simply cannot be typed from a terminal. Compose's test
 * input dispatches genuine multi-pointer events into the same gesture detector the
 * traveller's fingers reach.
 *
 * The assertion is deliberately about where a translation *lands on screen*, not
 * about the zoom variable: the whole risk in this feature is a translation drifting
 * off the words it belongs to, so the test measures the thing that would be wrong.
 */
class TranslationOverlayGestureTest {

    @get:Rule
    val compose = createComposeRule()

    private val state = TranslationState(
        imagePath = "",
        isLoading = false,
        targetLanguage = TranslateLanguage.ENGLISH,
        translation = TranslationResult(
            id = "test",
            imagePath = null,
            detectedLanguage = "Vietnamese",
            targetLanguage = "en",
            blocks = listOf(
                TranslationBlock(
                    original = "Bún chả Hà Nội",
                    translated = ANCHOR,
                    note = null,
                    price = null,
                    // Mid-frame so there is room to move in every direction.
                    box = TextBox(left = 0.2f, top = 0.4f, right = 0.7f, bottom = 0.46f),
                ),
            ),
            contextNote = null,
            createdAt = Instant.fromEpochSeconds(0),
        ),
    )

    @Test
    fun pinchingEnlargesTheTranslationWithThePhoto() {
        compose.setContent {
            SaolaTheme(themePreference = ThemePreference.DARK) {
                TranslationScreen(state = state, onIntent = {})
            }
        }
        compose.onNodeWithText(ANCHOR).assertExists()

        val before = overlayInk()
        compose.onRoot().performTouchInput { pinchOut() }
        compose.waitForIdle()
        val after = overlayInk()

        assertTrue(
            "Pinching out should draw the translation larger, ${before.pixels} px -> ${after.pixels} px",
            after.pixels > before.pixels * 1.5f,
        )
    }

    @Test
    fun draggingAZoomedPhotoMovesTheTranslationWithIt() {
        compose.setContent {
            SaolaTheme(themePreference = ThemePreference.DARK) {
                TranslationScreen(state = state, onIntent = {})
            }
        }
        compose.onNodeWithText(ANCHOR).assertExists()

        // Panning is deliberately inert until there is something off-screen to pan
        // to, so the drag only means anything after a zoom.
        compose.onRoot().performTouchInput { pinchOut() }
        compose.waitForIdle()
        val zoomed = overlayInk()

        compose.onRoot().performTouchInput {
            swipe(start = center, end = center + Offset(-DRAG, 0f), durationMillis = 200)
        }
        compose.waitForIdle()
        val panned = overlayInk()

        assertTrue(
            "Dragging left should carry the translation left, x ${zoomed.centreX} -> ${panned.centreX}",
            panned.centreX < zoomed.centreX - MIN_SHIFT,
        )
    }

    private fun TouchInjectionScope.pinchOut() = pinch(
        start0 = center + Offset(-60f, 0f),
        end0 = center + Offset(-PINCH, 0f),
        start1 = center + Offset(60f, 0f),
        end1 = center + Offset(PINCH, 0f),
    )

    /**
     * Where the white overlay type actually landed, measured off the rendered frame.
     *
     * Read from pixels rather than from the node's bounds because the zoom is a
     * draw-time transform: layout still reports the block at its original size, and
     * an assertion on that would pass whether or not the pinch did anything.
     *
     * The band excludes the top bar and the footer, whose white text never scales
     * and would otherwise dilute the count.
     *
     * Counting near-white only means anything against a dark background, which is why both
     * tests pin [ThemePreference.DARK] rather than letting [SaolaTheme] fall through to
     * `isSystemInDarkTheme()`. Left to the system setting these tests passed or failed on
     * whether the phone happened to be in night mode: in light mode the scheme's own
     * background is near-white, so this counted 1,203,485 "ink" pixels out of a 1,263,600-pixel
     * band — 95% of the screen — and the measurement was of the background, not the type.
     * The failure looked like a broken pinch and was a broken ruler.
     */
    private fun overlayInk(): Ink {
        val map = compose.onRoot().captureToImage().toPixelMap()
        val from = (map.height * BAND_TOP).toInt()
        val until = (map.height * BAND_BOTTOM).toInt()
        var pixels = 0
        var sumX = 0L
        for (y in from until until) {
            for (x in 0 until map.width) {
                val colour = map[x, y]
                if (colour.red > INK && colour.green > INK && colour.blue > INK) {
                    pixels++
                    sumX += x
                }
            }
        }
        return Ink(pixels = pixels, centreX = if (pixels == 0) 0f else sumX.toFloat() / pixels)
    }

    private data class Ink(val pixels: Int, val centreX: Float)

    private companion object {
        /** Long enough to be unmistakable, short enough not to be auto-shrunk away. */
        const val ANCHOR = "Grilled pork"

        /** How far each finger travels from the centre, in pixels. */
        const val PINCH = 320f
        const val DRAG = 250f

        /** Only near-white counts as overlay type. */
        const val INK = 0.75f

        /** The slice of the screen between the top bar and the footer. */
        const val BAND_TOP = 0.25f
        const val BAND_BOTTOM = 0.75f

        /** Enough movement to be a pan rather than a rounding difference. */
        const val MIN_SHIFT = 20f
    }
}
