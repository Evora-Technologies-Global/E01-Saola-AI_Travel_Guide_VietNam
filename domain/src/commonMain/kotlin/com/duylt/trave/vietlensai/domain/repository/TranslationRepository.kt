package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.model.TranslationResult
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/** OCR plus translation for menus, signs, leaflets and museum captions. */
interface TranslationRepository {

    /** @param sourceLanguage null when the traveller left the source on "detect". */
    suspend fun translate(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
        targetLanguage: TranslateLanguage,
    ): AppResult<TranslationResult>

    fun observeTranslations(): Flow<List<TranslationResult>>

    fun observeTranslation(id: String): Flow<TranslationResult?>

    suspend fun delete(id: String): AppResult<Unit>
}
