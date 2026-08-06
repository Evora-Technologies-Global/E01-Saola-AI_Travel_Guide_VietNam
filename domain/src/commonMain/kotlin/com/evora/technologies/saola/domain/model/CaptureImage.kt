package com.evora.technologies.saola.domain.model

/**
 * A capture, decoded the right way up and scaled to a workable size.
 *
 * Bytes rather than a platform bitmap: the same photo has to reach a Gemini request, an
 * on-device recogniser and a `Canvas` that clips it into a province outline, and encoded
 * JPEG is the one currency all three understand on both platforms.
 *
 * [widthPx] and [heightPx] are what make the translation overlay possible — a recogniser
 * reports boxes in pixels, and turning those into the fractions the UI draws with needs
 * the dimensions of the exact image that was read.
 */
class CaptureImage(
    val bytes: ByteArray,
    val widthPx: Int,
    val heightPx: Int,
) {
    /**
     * Equality by content, because the bytes are the value here.
     *
     * Hand-written rather than a `data class`: `ByteArray` equality is by reference, which
     * would make a generated `equals` quietly wrong for the one field that matters.
     */
    override fun equals(other: Any?): Boolean =
        this === other || (
            other is CaptureImage &&
                widthPx == other.widthPx &&
                heightPx == other.heightPx &&
                bytes.contentEquals(other.bytes)
            )

    override fun hashCode(): Int = 31 * (31 * bytes.contentHashCode() + widthPx) + heightPx

    override fun toString(): String = "CaptureImage(${widthPx}x$heightPx, ${bytes.size} bytes)"
}
