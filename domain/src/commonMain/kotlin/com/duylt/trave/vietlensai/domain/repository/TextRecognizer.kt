package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.RecognizedLine
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.util.AppResult

/**
 * Finds the text in a photo and says where each line is.
 *
 * A port rather than a use of the OCR library directly: reading the pixels is a
 * device capability, so the domain states what it needs — lines and their boxes —
 * and the data layer decides which recogniser, which script bundle, and how to
 * get the bitmap upright first.
 */
interface TextRecognizer {

    /**
     * @param sourceLanguage the language the text is expected to be in, or null to
     *   let the implementation work it out. It is a hint about *script*, not a
     *   filter: a Latin recogniser pointed at Japanese returns nonsense rather
     *   than nothing, which is why the caller's choice is worth passing down.
     */
    suspend fun recognize(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
    ): AppResult<List<RecognizedLine>>
}
