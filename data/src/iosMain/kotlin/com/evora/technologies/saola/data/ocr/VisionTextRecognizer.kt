package com.evora.technologies.saola.data.ocr

import com.evora.technologies.saola.data.local.file.toNSData
import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.RecognizedLine
import com.evora.technologies.saola.domain.model.TextBox
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.repository.TextRecognizer
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate

/**
 * Vision, the OCR engine built into iOS.
 *
 * Nothing to download, unlike ML Kit's per-script bundles — Vision ships with the OS
 * and supports every script this app cares about, so one pass covers what takes four
 * model bundles on Android. `recognitionLanguages` is a hint about script, exactly as
 * the ML Kit bundle choice is: a recogniser pointed at the wrong script returns
 * nonsense rather than nothing, which is why the traveller's own choice is passed down.
 */
@OptIn(ExperimentalForeignApi::class)
internal class VisionTextRecognizer(
    private val captureStore: CaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
) : TextRecognizer {

    override suspend fun recognize(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
    ): AppResult<List<RecognizedLine>> = withContext(ioDispatcher) {
        val decoded = when (val read = captureStore.read(imagePath)) {
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }

        try {
            val request = VNRecognizeTextRequest(completionHandler = null).apply {
                setRecognitionLevel(VNRequestTextRecognitionLevelAccurate)
                // Vietnamese diacritics are the whole game for a menu here, and the
                // language-correction pass is what recovers them from a blurry photo.
                setUsesLanguageCorrection(true)
                recognitionLanguagesFor(sourceLanguage)?.let { setRecognitionLanguages(it) }
            }

            val handler = VNImageRequestHandler(
                data = decoded.bytes.toNSData(),
                options = emptyMap<Any?, Any>(),
            )
            handler.performRequests(listOf(request), null)

            val observations = request.results
                ?.filterIsInstance<VNRecognizedTextObservation>()
                .orEmpty()

            AppResult.Success(observations.mapNotNull { it.toRecognizedLine() })
        } catch (e: Exception) {
            log.e(e) { "On-device text recognition failed for $imagePath" }
            AppResult.Failure(AppError.ImageUnavailable(e.message))
        }
    }

    /**
     * Vision reports `boundingBox` in normalised coordinates with the origin at the
     * *bottom* left, while [TextBox] — and every canvas that draws it — measures from
     * the top. Flipping here rather than at the call site keeps the two platforms'
     * recognisers interchangeable.
     */
    private fun VNRecognizedTextObservation.toRecognizedLine(): RecognizedLine? {
        val text = topCandidates(1u)
            .firstOrNull()
            ?.let { (it as? platform.Vision.VNRecognizedText)?.string }
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return boundingBox.useContents {
            RecognizedLine(
                text = text,
                box = TextBox(
                    left = origin.x.toFloat(),
                    top = (1.0 - (origin.y + size.height)).toFloat(),
                    right = (origin.x + size.width).toFloat(),
                    bottom = (1.0 - origin.y).toFloat(),
                ),
            )
        }
    }

    /**
     * Which scripts to tell Vision to expect, or null to leave it on its own default.
     *
     * A stated source language is taken at its word. On "detect" the list leads with
     * Vietnamese and English — this is an app for Vietnam, and all but a fraction of
     * what gets photographed here is one of those two — then the CJK scripts, which
     * Vision only reaches for when the leading ones find nothing legible.
     */
    private fun recognitionLanguagesFor(source: TranslateLanguage?): List<String>? =
        when (source) {
            null -> listOf("vi-VN", "en-US", "zh-Hans", "ja-JP", "ko-KR")
            TranslateLanguage.VIETNAMESE -> listOf("vi-VN")
            TranslateLanguage.ENGLISH -> listOf("en-US")
            TranslateLanguage.CHINESE -> listOf("zh-Hans", "zh-Hant")
            TranslateLanguage.JAPANESE -> listOf("ja-JP")
            TranslateLanguage.KOREAN -> listOf("ko-KR")
            TranslateLanguage.FRENCH -> listOf("fr-FR")
            TranslateLanguage.SPANISH -> listOf("es-ES")
            TranslateLanguage.THAI -> listOf("th-TH")
        }
}
