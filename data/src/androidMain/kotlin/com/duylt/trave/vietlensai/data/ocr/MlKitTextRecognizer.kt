package com.duylt.trave.vietlensai.data.ocr

import com.duylt.trave.vietlensai.data.local.file.AndroidCaptureStore
import com.duylt.trave.vietlensai.domain.model.RecognizedLine
import com.duylt.trave.vietlensai.domain.model.TextBox
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.repository.TextRecognizer
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.util.AppResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import com.google.mlkit.vision.text.TextRecognizer as MlKitRecognizer

/**
 * On-device OCR, one bundled model per script.
 *
 * The boxes are the reason this runs locally at all: the translator could read the photo
 * by itself, but a language model asked to also report pixel coordinates guesses them,
 * and a translation pinned half a line off the text it belongs to is worse than no
 * overlay. The recogniser knows exactly where it looked.
 */
internal class MlKitTextRecognizer(
    private val captureStore: AndroidCaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
) : TextRecognizer {

    // Built once and kept: each client loads a model, and rebuilding one per photo
    // adds a visible pause to the first shot of every capture. Concurrent because
    // two captures can be in flight when the traveller shoots twice in a second.
    private val clients = ConcurrentHashMap<Script, MlKitRecognizer>()

    override suspend fun recognize(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
    ): AppResult<List<RecognizedLine>> = withContext(ioDispatcher) {
        val bitmap = when (val read = captureStore.readOriented(imagePath)) {
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }

        try {
            // Rotation is already baked into the bitmap, so zero degrees here is a
            // statement of fact: it keeps the returned boxes in the same coordinate
            // space as the picture the traveller is looking at.
            val image = InputImage.fromBitmap(bitmap, 0)
            val lines = scriptsToTry(sourceLanguage).firstNotNullOfOrNull { script ->
                read(image, script, bitmap.width, bitmap.height).takeIf { it.isNotEmpty() }
            }
            AppResult.Success(lines.orEmpty())
        } catch (e: Exception) {
            log.e(e) { "On-device text recognition failed for $imagePath" }
            AppResult.Failure(AppError.ImageUnavailable(e.message))
        } finally {
            bitmap.recycle()
        }
    }

    private suspend fun read(
        image: InputImage,
        script: Script,
        widthPx: Int,
        heightPx: Int,
    ): List<RecognizedLine> {
        val recognized = client(script).process(image).await()
        return recognized.textBlocks
            .flatMap { block -> block.lines }
            .mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                val text = line.text.trim()
                if (text.isEmpty()) return@mapNotNull null
                RecognizedLine(
                    text = text,
                    box = TextBox(
                        left = bounds.left.toFloat() / widthPx,
                        top = bounds.top.toFloat() / heightPx,
                        right = bounds.right.toFloat() / widthPx,
                        bottom = bounds.bottom.toFloat() / heightPx,
                    ),
                )
            }
    }

    private fun client(script: Script): MlKitRecognizer = clients.getOrPut(script) {
        TextRecognition.getClient(script.options())
    }

    /**
     * Which models to run, in order, stopping at the first that finds anything.
     *
     * A stated source language is taken at its word — one pass, no guessing. On
     * "detect", Latin goes first because this is an app for Vietnam and all but a
     * fraction of what gets photographed here is Latin script; the others follow
     * only when a photo comes back empty, which is the one case worth the wait.
     */
    private fun scriptsToTry(sourceLanguage: TranslateLanguage?): List<Script> =
        when (sourceLanguage) {
            null -> listOf(Script.LATIN, Script.CHINESE, Script.JAPANESE, Script.KOREAN)
            TranslateLanguage.CHINESE -> listOf(Script.CHINESE)
            TranslateLanguage.JAPANESE -> listOf(Script.JAPANESE)
            TranslateLanguage.KOREAN -> listOf(Script.KOREAN)
            // Thai has no bundle of its own in ML Kit, so it falls here with the
            // Latin languages and comes back transliterated at best.
            else -> listOf(Script.LATIN)
        }

    private enum class Script {
        LATIN,
        CHINESE,
        JAPANESE,
        KOREAN,
        ;

        fun options(): TextRecognizerOptionsInterface = when (this) {
            LATIN -> TextRecognizerOptions.DEFAULT_OPTIONS
            CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            KOREAN -> KoreanTextRecognizerOptions.Builder().build()
        }
    }
}
