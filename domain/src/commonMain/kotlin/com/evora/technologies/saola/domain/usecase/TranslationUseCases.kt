package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.domain.repository.TranslationRepository
import com.evora.technologies.saola.domain.util.AppResult

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
