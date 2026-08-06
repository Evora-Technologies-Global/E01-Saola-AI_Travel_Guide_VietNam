package com.evora.technologies.saola.feature.camera

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.util.AppError

data class LensState(
    val mode: LensMode = LensMode.AUTO,
    /**
     * What the mode dropdown offers.
     *
     * Translate is one of them: it captures into a translation overlay rather than
     * a discovery, but from the traveller's side it is still "point the camera at
     * the thing and press the button", so it belongs in the same picker as the
     * rest. The view never decides which modes exist — this list does.
     */
    val availableModes: List<LensMode> = LensMode.entries,
    /**
     * Which language the sign is in — null for "work it out from the photo".
     *
     * Null rather than an AUTO entry in the enum: "detect it" is not a language,
     * and only the source side may be left open. A target of "whatever" would have
     * nothing to translate into.
     */
    val translateFrom: TranslateLanguage? = null,
    val translateTo: TranslateLanguage = TranslateLanguage.ENGLISH,
    val isAnalysing: Boolean = false,
    /** Which line of the "what the AI is doing" copy is showing. */
    val analysisStage: Int = 0,
    val flashEnabled: Boolean = false,
    val gridEnabled: Boolean = false,
    val lensFacing: LensFacing = LensFacing.BACK,
    val timer: CaptureTimer = CaptureTimer.OFF,
    /** Seconds left on the self-timer; zero when it is not running. */
    val countdown: Int = 0,
    /**
     * The shutter has fired but the JPEG has not landed yet.
     *
     * Its own flag rather than a corner of [isAnalysing]: analysis only starts once
     * there is a file, which leaves a few hundred milliseconds in which a second
     * press would start a second, overlapping capture.
     */
    val isCapturing: Boolean = false,
    val hasApiKey: Boolean = true,
    /**
     * Whether the location request has already been put to this traveller.
     *
     * Defaults to true, which is the opposite of the stored default on purpose:
     * settings arrive one emission after the first frame, and starting at false
     * would flash the location card onto the viewfinder for that frame on every
     * single launch, including for people who granted the permission months ago.
     */
    val hasAskedLocation: Boolean = true,
    val recentDiscoveries: List<Discovery> = emptyList(),
    val error: AppError? = null,
) : UiState {
    val isCountingDown: Boolean get() = countdown > 0

    /** True whenever a second shutter press would be wrong rather than merely early. */
    val isBusy: Boolean get() = isAnalysing || isCountingDown || isCapturing
}

sealed interface LensIntent : UiIntent {
    data class SelectMode(val mode: LensMode) : LensIntent

    /** Null is the picker's "detect it for me" entry, not an absent choice. */
    data class SelectTranslateFrom(val language: TranslateLanguage?) : LensIntent
    data class SelectTranslateTo(val language: TranslateLanguage) : LensIntent
    data class PhotoCaptured(val path: String) : LensIntent
    /**
     * A photo chosen from the library, already copied into app storage.
     *
     * A path rather than a platform URI: the picker's own handle is only valid for as long
     * as the system keeps its temporary grant, so the copy happens before this is raised and
     * the rest of the pipeline cannot tell a picked photo from a captured one.
     */
    data class PhotoPicked(val path: String) : LensIntent
    data class CaptureFailed(val message: String?) : LensIntent

    /**
     * The capture coroutine died with the screen: no photo, and nothing to apologise
     * for. Distinct from [CaptureFailed] so leaving mid-capture does not leave an
     * error banner waiting on the way back.
     */
    data object CaptureAborted : LensIntent

    /**
     * The screen started, and the volume keys should mean "shutter" again.
     *
     * Claimed and released with the screen rather than for the app's lifetime: a
     * traveller reading a discovery still expects those keys to change the volume.
     */
    data object ScreenStarted : LensIntent

    /**
     * The screen stopped.
     *
     * A self-timer belongs to the frame being composed; once the traveller has
     * tabbed away there is nothing left to photograph, so the countdown is dropped
     * rather than fired at a camera that is no longer bound.
     */
    data object ScreenStopped : LensIntent

    /**
     * The shutter, not the capture.
     *
     * With a self-timer the two stop being the same event: the press starts a
     * countdown the ViewModel owns, and pressing again during it cancels rather
     * than queues a second photo.
     */
    data object ShutterPressed : LensIntent
    data object ToggleFlash : LensIntent
    data object ToggleGrid : LensIntent
    data object SwitchLens : LensIntent
    data object CycleTimer : LensIntent

    /**
     * Read the sign the other way round.
     *
     * A no-op while the source is still "detect it": there is no known language to
     * move across, so the button is dimmed rather than made to guess one.
     */
    data object SwapTranslateLanguages : LensIntent
    data object DismissError : LensIntent

    /**
     * The location card has been answered — allowed or declined, it makes no
     * difference here.
     *
     * What gets recorded is that the question was asked, not what the answer was.
     * The answer already lives in the OS permission, and keeping a copy of it in
     * app storage is exactly the mistake that hid this whole bug: two records of
     * one fact, one of which was never true.
     */
    data object LocationAsked : LensIntent
}

sealed interface LensEffect : UiEffect {
    data class OpenDiscovery(val id: String) : LensEffect
    /**
     * Hand the photo straight to the translation screen, untranslated.
     *
     * The path and the pair rather than a finished result's id: reading a sign is
     * the one flow where the traveller has nothing to look at while they wait, so
     * the screen opens on their own photo immediately and does the asking itself.
     */
    data class OpenTranslation(
        val imagePath: String,
        val from: TranslateLanguage?,
        val to: TranslateLanguage,
    ) : LensEffect
    data class ShowMessage(val error: AppError) : LensEffect

    /**
     * Fire the shutter now, into [outputPath].
     *
     * The camera hardware is held by the screen, so the ViewModel decides *when*
     * a photo is taken and the screen decides *how* — that split is what lets the
     * self-timer live in tested, lifecycle-safe code instead of in a composable.
     *
     * The destination travels with the instruction rather than being fetched by the
     * screen from a public method on the ViewModel: the same moment already decides
     * *when* the shutter fires, and a composable that has to call back for *where*
     * is a second entry point into a ViewModel that is supposed to have one.
     * [com.evora.technologies.saola.domain.repository.CaptureStore] stays the only
     * thing that knows captures live in a particular app-storage directory.
     */
    data class TakePhoto(val outputPath: String) : LensEffect
}
