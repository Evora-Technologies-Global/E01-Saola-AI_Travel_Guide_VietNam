package com.duylt.trave.vietlensai.domain.model

import kotlin.time.Instant

/**
 * The outcome of pointing the camera at a menu, a street sign or a museum caption.
 *
 * Kept block-by-block rather than as one blob of text so the UI can show the
 * original line next to its translation — which is what makes a menu usable when
 * you still have to say the Vietnamese name out loud to the waiter.
 */
data class TranslationResult(
    val id: String,
    val imagePath: String?,
    val detectedLanguage: String,
    val targetLanguage: String,
    val blocks: List<TranslationBlock>,
    val contextNote: String?,
    val createdAt: Instant,
) {
    val originalText: String get() = blocks.joinToString(separator = "\n") { it.original }
    val translatedText: String get() = blocks.joinToString(separator = "\n") { it.translated }
}

data class TranslationBlock(
    val original: String,
    val translated: String,
    /** Optional cultural gloss, e.g. "a northern-style noodle soup, usually breakfast". */
    val note: String?,
    /** Price if the block is a menu line, already formatted, e.g. "45.000 ₫". */
    val price: String?,
    /**
     * Where the original text sits in the photo, when it is known.
     *
     * Nullable because it comes from the on-device recogniser rather than from the
     * translation itself: a result restored from an older capture has none, and a
     * screen that draws over the photo has to fall back to a list for those.
     */
    val box: TextBox? = null,
)
