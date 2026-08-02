package com.duylt.trave.vietlensai.domain.usecase

import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.model.TranslationResult
import com.duylt.trave.vietlensai.domain.repository.TranslationRepository
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * Translates whatever text is in the photo into the language the traveller asked for.
 *
 * The pair is passed in rather than read from settings: the app's own language is
 * what it speaks *to* the traveller, and someone reading English while standing in
 * front of a Japanese temple sign needs to say so without changing the whole app.
 * A null source means "work it out from the photo".
 */
class TranslateImageUseCase(
    private val translationRepository: TranslationRepository,
) {
    suspend operator fun invoke(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
        targetLanguage: TranslateLanguage,
    ): AppResult<TranslationResult> = translationRepository.translate(
        imagePath = imagePath,
        sourceLanguage = sourceLanguage,
        targetLanguage = targetLanguage,
    )
}

class ObserveTranslationsUseCase(
    private val repository: TranslationRepository,
) {
    operator fun invoke(): Flow<List<TranslationResult>> = repository.observeTranslations()
}

class ObserveTranslationUseCase(
    private val repository: TranslationRepository,
) {
    operator fun invoke(id: String): Flow<TranslationResult?> = repository.observeTranslation(id)
}

class DeleteTranslationUseCase(
    private val repository: TranslationRepository,
) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.delete(id)
}
