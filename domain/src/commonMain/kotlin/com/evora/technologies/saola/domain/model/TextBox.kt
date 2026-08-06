package com.evora.technologies.saola.domain.model

/**
 * Where a piece of text sits in the photo, as fractions of its width and height.
 *
 * Normalised rather than in pixels because the photo is drawn at a different size
 * everywhere it appears — full screen, zoomed, or as a thumbnail — and a box in
 * pixels would have to be rescaled correctly at every one of those call sites.
 * As a fraction it is simply multiplied by whatever the picture is drawn at.
 */
data class TextBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** One line of text as the on-device recogniser found it, before translation. */
data class RecognizedLine(
    val text: String,
    val box: TextBox,
)
