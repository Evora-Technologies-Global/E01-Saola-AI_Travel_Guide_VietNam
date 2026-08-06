package com.evora.technologies.saola.feature.camera

import kotlin.math.ln
import kotlin.math.pow

/**
 * Camera options the traveller chooses, and what the device answers back.
 *
 * These live beside the screen rather than in `domain`: none of them survive the
 * session or reach a use case. Which way the lens points is a fact about this
 * phone in this moment, not something the recognition pipeline has an opinion on.
 */

/** Which way the camera points. */
enum class LensFacing {
    BACK,
    FRONT,
    ;

    fun flipped(): LensFacing = if (this == BACK) FRONT else BACK
}

/**
 * Delay between pressing the shutter and the capture.
 *
 * Three and ten, the two values every phone camera offers: three to steady the
 * phone against a railing, ten to get into the frame in front of the pagoda.
 */
enum class CaptureTimer(val seconds: Int) {
    OFF(0),
    THREE(3),
    TEN(10),
    ;

    fun next(): CaptureTimer = entries[(ordinal + 1) % entries.size]
}

/**
 * What the currently bound camera can actually do.
 *
 * Read from the hardware, never chosen by the traveller — which is why it is not
 * part of `LensState`. The ViewModel holds no camera handle and could neither
 * produce these values nor act on them; the screen reads them straight from
 * [CameraController] and hides the controls the device cannot honour.
 */
data class CameraCapabilities(
    val hasFlash: Boolean = false,
    val hasFrontCamera: Boolean = false,
    val zoomRatio: Float = 1f,
    val minZoomRatio: Float = 1f,
    val maxZoomRatio: Float = 1f,
    /** Exposure compensation, in the camera's own index units; empty when unsupported. */
    val exposureRange: IntRange = IntRange.EMPTY,
    /** How many EV one index step is worth — a third of a stop on most phones. */
    val exposureStepEv: Float = 0f,
    val exposureIndex: Int = 0,
) {
    val canZoom: Boolean get() = maxZoomRatio > minZoomRatio

    val canAdjustExposure: Boolean get() = !exposureRange.isEmpty() && exposureStepEv > 0f

    /** The current compensation as the traveller reads it: "+1.0", not "+3". */
    val exposureEv: Float get() = exposureIndex * exposureStepEv

    /**
     * Where the current ratio sits in the range, 0 to 1, on a log scale.
     *
     * Logarithmic because zoom is perceived in doublings: 1×→2× has to feel like
     * the same distance as 2×→4×, or the far end of the dial becomes unusable.
     */
    fun zoomProgress(): Float = zoomProgressOf(zoomRatio)

    fun zoomProgressOf(ratio: Float): Float {
        if (!canZoom) return 0f
        return (ln(ratio / minZoomRatio) / ln(maxZoomRatio / minZoomRatio)).coerceIn(0f, 1f)
    }

    /** The inverse of [zoomProgress]: the ratio at a point along the dial. */
    fun zoomRatioAt(progress: Float): Float {
        if (!canZoom) return zoomRatio
        return minZoomRatio * (maxZoomRatio / minZoomRatio).pow(progress.coerceIn(0f, 1f))
    }

    /**
     * The zoom steps worth offering as buttons.
     *
     * Filtered against the real range so an ultrawide phone gets its 0.5× and a
     * phone with a single lens is not offered a 5× that silently does nothing.
     */
    fun zoomStops(): List<Float> {
        if (!canZoom) return emptyList()
        val stops = CANDIDATE_STOPS.filter { it >= minZoomRatio && it <= maxZoomRatio }
        return if (stops.size > 1) stops else emptyList()
    }

    private companion object {
        val CANDIDATE_STOPS = listOf(0.5f, 1f, 2f, 5f)
    }
}
