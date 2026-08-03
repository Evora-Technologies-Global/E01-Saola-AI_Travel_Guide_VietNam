package com.duylt.trave.vietlensai

import androidx.test.platform.app.InstrumentationRegistry
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.repository.ChatRepository
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.TranslationRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

/**
 * Exercises the real pipeline against the real Gemini API: a photo on disk goes in,
 * a persisted [com.duylt.trave.vietlensai.domain.model.Discovery] comes out.
 *
 * This is the one test that proves the parts fit together — schema, prompt, model
 * routing, mapper and Room. Unit tests can only show each link works in isolation.
 *
 * Because it costs real quota and needs a network, every case skips (rather than
 * fails) when the model is genuinely unreachable, so a busy Gemini or an offline
 * CI box never produces a misleading red build.
 *
 * **Every assertion here is language-agnostic, and has to be.** The guide answers in the
 * phone's language and that is not settable — `SettingsRepository` has no `setLanguage`,
 * because `AppSettings.language` is read from the device. So each case matches its subject
 * across both the English and the Vietnamese name rather than pinning one: run on a
 * Vietnamese emulator the landmark comes back as "Văn Miếu", on an English one as "Temple
 * of Literature", and neither is the wrong answer.
 */
class RecognitionEndToEndTest : KoinComponent {

    /*
     * Resolved from the running application's Koin graph rather than injected into a
     * per-test component.
     *
     * That is a real simplification over the Hilt version, not just a translation of it:
     * Hilt built a fresh `SingletonComponent` per test method, which meant a fresh DataStore
     * over the same file — and DataStore refuses two live instances on one file, so the old
     * scope had to be cancelled in a teardown. Koin keeps one graph for the process, so there
     * is one DataStore and nothing to tear down.
     */
    private val discoveryRepository: DiscoveryRepository by inject()
    private val translationRepository: TranslationRepository by inject()
    private val chatRepository: ChatRepository by inject()
    private val captureStore: CaptureStore by inject()

    @Test
    fun recognisesAVietnameseLandmarkAndStoresIt() = runTest(timeout = TEST_TIMEOUT) {
        val path = copyAssetToStorage("temple_of_literature.jpg")

        val result = discoveryRepository.recognize(
            imagePath = path,
            mode = LensMode.LANDMARK,
            location = null,
        )
        val discovery = result.assumeReachable() ?: return@runTest

        /*
         * Asserted across the whole record rather than on the title alone.
         * A correct answer may name the site ("Temple of Literature") or the
         * specific structure in frame ("Khuê Văn Các" / "Pavilion of the
         * Constellation of Literature") — the second is *more* precise, and a
         * title-only assertion would punish the better answer.
         */
        val haystack = listOfNotNull(
            discovery.title,
            discovery.localName,
            discovery.placeHint,
            discovery.summary,
        ).joinToString(" ")
        assertTrue(
            "Expected the Temple of Literature complex, got '${discovery.title}'",
            listOf("temple of literature", "văn miếu", "van mieu", "khuê văn", "khue van")
                .any { haystack.contains(it, ignoreCase = true) },
        )
        assertEquals(DiscoveryCategory.LANDMARK, discovery.category)
        assertTrue("Confidence should be high for a famous landmark", discovery.confidence > 0.5f)
        assertTrue("Summary should not be empty", discovery.summary.isNotBlank())
        assertTrue("Expected narrative sections", discovery.sections.isNotEmpty())
        // No assertion on suggestedQuestions or nearbySuggestions: the recognition
        // schema stopped asking for them to keep the capture wait inside its budget,
        // so both are legitimately empty on a fresh record.

        // Room is the single source of truth: the result screen and the journal
        // both read the same row that was just written.
        val stored = discoveryRepository.observeDiscoveries().first()
        assertTrue(stored.any { it.id == discovery.id })
    }

    @Test
    fun recognisesAVietnameseDishAsFood() = runTest(timeout = TEST_TIMEOUT) {
        val path = copyAssetToStorage("pho.jpg")

        val result = discoveryRepository.recognize(
            imagePath = path,
            mode = LensMode.FOOD,
            location = null,
        )
        val discovery = result.assumeReachable() ?: return@runTest

        assertEquals(DiscoveryCategory.FOOD, discovery.category)

        // Which field holds which name depends on the phone: in English the title is
        // "Beef Noodle Soup" and "phở" lands in localName, in Vietnamese they swap. The
        // dish therefore has to be identified across the record, not from the title alone.
        val haystack = listOfNotNull(discovery.title, discovery.localName, discovery.summary)
            .joinToString(" ")
        assertTrue(
            "Expected phở, got '${discovery.title}' / '${discovery.localName}'",
            listOf("phở", "pho ", "noodle soup").any { haystack.contains(it, ignoreCase = true) },
        )
        assertTrue(discovery.sections.isNotEmpty())
    }

    @Test
    fun answersAFollowUpQuestionInContext() = runTest(timeout = TEST_TIMEOUT) {
        val path = copyAssetToStorage("temple_of_literature.jpg")

        val discovery = discoveryRepository
            .recognize(path, LensMode.LANDMARK, null)
            .assumeReachable() ?: return@runTest

        // Deliberately elliptical: only a model that received the discovery as
        // context can resolve "this" to the Temple of Literature.
        val answer = chatRepository.ask(discovery.id, "Who built this, and when?")
            .assumeReachable() ?: return@runTest

        assertTrue("Answer should not be empty", answer.content.isNotBlank())

        val thread = chatRepository.observeMessages(discovery.id).first()
        assertEquals("Question and answer should both be stored", 2, thread.size)
    }

    @Test
    fun readsAndTranslatesTextInAPhoto() = runTest(timeout = TEST_TIMEOUT) {
        val path = copyAssetToStorage("temple_of_literature.jpg")

        // Source left null on purpose: "detect it" is the default the camera ships
        // with, so it is the path worth proving against a real photo.
        val result = translationRepository.translate(
            imagePath = path,
            sourceLanguage = null,
            targetLanguage = TranslateLanguage.ENGLISH,
        )
        when (result) {
            is AppResult.Success -> {
                assertTrue(result.data.blocks.isNotEmpty())
                assertTrue(result.data.detectedLanguage.isNotBlank())
            }
            // A photo with no legible signage is a legitimate outcome for this
            // fixture; what matters is that it is reported as "nothing to read"
            // rather than as a crash or a malformed payload.
            is AppResult.Failure -> assertTrue(
                "Unexpected translation failure: ${result.error}",
                result.error is AppError.NotRecognized || result.error.isEnvironmental(),
            )
        }
    }

    private fun copyAssetToStorage(assetName: String): String {
        val context = InstrumentationRegistry.getInstrumentation().context
        val target = File(captureStore.newCapturePath())
        context.assets.open(assetName).use { input ->
            File(target.absolutePath).outputStream().use { output -> input.copyTo(output) }
        }
        return target.absolutePath
    }

    /**
     * Unwraps a success, or skips the test when the failure is about the
     * environment (offline, no quota, every model busy) rather than about the code.
     */
    private fun <T> AppResult<T>.assumeReachable(): T? {
        if (this is AppResult.Success) return data
        val error = (this as AppResult.Failure).error
        assumeTrue("Skipping: Gemini unreachable ($error)", !error.isEnvironmental())
        throw AssertionError("Pipeline failed for a non-environmental reason: $error")
    }

    private fun AppError.isEnvironmental(): Boolean = when (this) {
        AppError.NoConnection,
        AppError.Timeout,
        AppError.MissingApiKey,
        AppError.InvalidApiKey,
        is AppError.RateLimited,
        is AppError.AllModelsBusy,
        -> true
        is AppError.Api -> code == 429 || code >= 500
        else -> false
    }

    private companion object {
        /** Generous: a Pro-tier fallback under load genuinely takes this long. */
        val TEST_TIMEOUT = kotlin.time.Duration.parse("180s")
    }
}
