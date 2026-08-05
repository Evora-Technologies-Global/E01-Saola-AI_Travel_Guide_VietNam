package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.domain.util.AppResult
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
