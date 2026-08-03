package com.duylt.trave.vietlensai.data.local.file

/**
 * The encoding policy both platforms honour, so a capture is the same size on the wire
 * wherever it was taken.
 *
 * Downscaling before upload is the single biggest lever on this app's cost and latency: a
 * full 12 MP capture is ~1.5 MB of base64 and buys no extra recognition accuracy over a
 * 1024 px long edge, while costing seconds on a hotel Wi-Fi.
 */
internal object ImagePolicy {
    const val MAX_EDGE_PX = 1024
    const val JPEG_QUALITY = 85
    const val MIME_JPEG = "image/jpeg"
    const val DIRECTORY = "captures"

    /**
     * The `BitmapFactory.Options.inSampleSize` to decode a capture whose longest edge is
     * [longestEdgePx].
     *
     * A power of two, because that is the only thing the decoder honours exactly — anything
     * else is rounded down to one, so asking for 3 silently gets 2. The value returned is the
     * largest power of two that still leaves the image at or above [MAX_EDGE_PX], which is
     * deliberate: sampling past the target throws away detail the recogniser wants and cannot
     * be undone, so the remainder is taken off afterwards by [scaleNumerator] / [scaleDenominator]
     * where the resampling is a proper filtered one.
     *
     * Android-only in practice, but it lives here rather than in `AndroidCaptureStore` because
     * this object is where the promise is made that "a capture is the same size on the wire
     * wherever it was taken" — and it is only checkable if both halves of the arithmetic are
     * in the same place as the constant they are about.
     */
    fun sampleSizeFor(longestEdgePx: Int): Int {
        if (longestEdgePx <= 0) return 1
        var sampleSize = 1
        while (longestEdgePx / (sampleSize * 2) >= MAX_EDGE_PX) {
            sampleSize *= 2
        }
        return sampleSize
    }

    /**
     * The longest edge a decode at [sampleSizeFor] leaves behind.
     *
     * Between [MAX_EDGE_PX] and just under twice it, which is the whole point of the second
     * step: a 4032 px capture samples to 2016 px, and 2016 x 1512 in ARGB_8888 is 12 MB of
     * bitmap where iOS — which resizes to exactly [MAX_EDGE_PX] through `draw(in:)` — holds
     * 3 MB. That was a real four-fold difference in peak memory between the two platforms for
     * the same photograph, on the one code path that runs while the camera is still bound.
     */
    fun decodedEdgeFor(longestEdgePx: Int): Int =
        if (longestEdgePx <= 0) 0 else longestEdgePx / sampleSizeFor(longestEdgePx)

    /**
     * Whether a decoded bitmap still needs the final, exact resize.
     *
     * False for a capture that was already small enough, so a photo picked from the library at
     * 800 px is never scaled *up* — enlarging costs memory and invents nothing.
     */
    fun needsExactResize(decodedEdgePx: Int): Boolean = decodedEdgePx > MAX_EDGE_PX

    /**
     * The size [edgePx] becomes once the longest edge is clamped to [MAX_EDGE_PX].
     *
     * Applied to both edges from the same ratio so the aspect ratio is preserved; a stretched
     * capture would move every OCR box off the words it belongs to.
     */
    fun clampedEdge(edgePx: Int, longestEdgePx: Int): Int {
        if (longestEdgePx <= MAX_EDGE_PX) return edgePx
        // Rounded rather than truncated, and floored at 1: a 4000x9 panorama must not clamp
        // its short edge to zero, which is a decoder crash rather than a small image.
        val scaled = (edgePx.toLong() * MAX_EDGE_PX + longestEdgePx / 2) / longestEdgePx
        return scaled.toInt().coerceAtLeast(1)
    }

    private const val CAPTURE_PREFIX = "capture_"
    private const val CAPTURE_EXTENSION = ".jpg"

    /**
     * The one place a capture is named.
     *
     * The creation time is carried in the name rather than read from the filesystem
     * because the orphan sweep needs it, and file metadata is one more thing that would
     * have to be expressed differently on each platform to get at.
     */
    fun captureFileName(epochMillis: Long): String =
        "$CAPTURE_PREFIX$epochMillis$CAPTURE_EXTENSION"

    /**
     * The bare file name of [nameOrPath], whichever of the two it already was.
     *
     * Both platforms strip a directory the same way, so the rule lives here beside the
     * naming it undoes rather than twice in the two stores. `file://` is trimmed first
     * because the iOS picker hands back a URL, and a scheme left on the front would be
     * stored as part of the name.
     */
    fun fileNameOf(nameOrPath: String): String =
        nameOrPath.removePrefix("file://").substringAfterLast('/')

    /**
     * When the capture at [path] was created, or null if the name did not come from
     * [captureFileName].
     *
     * Null is deliberately treated as "do not touch" by the sweep: a file this cannot
     * date is a file it cannot prove is stale.
     */
    fun capturedAtMillis(path: String): Long? = path
        .substringAfterLast('/')
        .takeIf { it.startsWith(CAPTURE_PREFIX) && it.endsWith(CAPTURE_EXTENSION) }
        ?.removePrefix(CAPTURE_PREFIX)
        ?.removeSuffix(CAPTURE_EXTENSION)
        ?.toLongOrNull()
}
