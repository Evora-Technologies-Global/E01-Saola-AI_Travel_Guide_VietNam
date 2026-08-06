package com.evora.technologies.saola.feature.discovery

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.util.AppError

data class DiscoveryState(
    val isLoading: Boolean = true,
    val discovery: Discovery? = null,
    val language: AppLanguage = AppLanguage.VIETNAMESE,
    val isSpeaking: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val note: DiscoveryNote? = null,
    /** Non-null only while the composer is open; the saved [note] is untouched until it closes. */
    val noteEditor: NoteEditor? = null,
    val isSavingNote: Boolean = false,
    /** The objection already on file for this discovery, if the traveller has filed one. */
    val report: DiscoveryReport? = null,
    /** Non-null only while the report sheet is open — the same shape as [noteEditor]. */
    val reportDraft: ReportDraft? = null,
    val isSubmittingReport: Boolean = false,
) : UiState {
    val isEditingNote: Boolean get() = noteEditor != null

    val isReporting: Boolean get() = reportDraft != null
}

/**
 * The report sheet's unsaved contents.
 *
 * Held apart from [DiscoveryState.report] for the reason [NoteEditor] is held apart from the
 * saved note: backing out of a re-report has to leave the objection already on file exactly as
 * it was, and the footer reads that field while the sheet is open on top of it.
 *
 * [reason] is nullable because the sheet opens with nothing chosen. That is the whole of what
 * [canSubmit] guards — the note is optional even for [ReportReason.OTHER], since "this is
 * wrong and I can't say why" is a report worth having, and demanding a sentence for it is how
 * a complaint gets abandoned halfway.
 */
data class ReportDraft(
    val reason: ReportReason? = null,
    val note: String = "",
) {
    val canSubmit: Boolean get() = reason != null

    /** How much of the cap `SubmitReportUseCase` will apply is already used up. */
    val noteLength: Int get() = note.length
}

/**
 * The composer's unsaved contents.
 *
 * Held apart from [DiscoveryState.note] rather than mutating it, so backing out of an edit
 * restores what was written before without a round trip to the database — and so the note
 * on screen never flickers through a half-typed state while the traveller is editing it.
 */
data class NoteEditor(
    val body: String,
    val photoPaths: List<String>,
) {
    val canAddPhoto: Boolean get() = photoPaths.size < DiscoveryNote.MAX_PHOTOS
    val isEmpty: Boolean get() = body.isBlank() && photoPaths.isEmpty()
}

sealed interface DiscoveryIntent : UiIntent {
    data object ToggleFavorite : DiscoveryIntent
    data object ToggleSpeech : DiscoveryIntent
    data object RequestDelete : DiscoveryIntent
    data object CancelDelete : DiscoveryIntent
    data object ConfirmDelete : DiscoveryIntent
    data class AskSuggested(val question: String) : DiscoveryIntent

    data object StartEditNote : DiscoveryIntent
    data object CancelEditNote : DiscoveryIntent
    data class NoteBodyChanged(val body: String) : DiscoveryIntent

    /** @param paths already inside app storage — the picker imports before this is sent. */
    data class NotePhotoPicked(val paths: List<String>) : DiscoveryIntent

    /**
     * A photo just taken with the in-app camera.
     *
     * Separate from [NotePhotoPicked] because a fresh capture carries its rotation as an
     * EXIF tag rather than in its pixels, and has to be rewritten upright before anything
     * displays it. A photo out of the gallery has already been through that once.
     *
     * @param path the JPEG the camera wrote, inside app storage.
     */
    data class NotePhotoCaptured(val path: String) : DiscoveryIntent

    data class NotePhotoRemoved(val path: String) : DiscoveryIntent
    data object SaveNote : DiscoveryIntent
    data object DeleteNote : DiscoveryIntent

    /**
     * The traveller says the result is wrong.
     *
     * Opens the sheet; it does not file anything. Named for what they did rather than for the
     * sheet it raises, so that a second way in — the low-confidence note, if it ever grows a
     * link — sends the same intent rather than a second one meaning the same thing.
     */
    data object StartReport : DiscoveryIntent
    data object CancelReport : DiscoveryIntent
    data class ReportReasonSelected(val reason: ReportReason) : DiscoveryIntent
    data class ReportNoteChanged(val note: String) : DiscoveryIntent
    data object SubmitReport : DiscoveryIntent
}

sealed interface DiscoveryEffect : UiEffect {
    data object NavigateBack : DiscoveryEffect
    data class OpenChat(val discoveryId: String, val prefill: String?) : DiscoveryEffect

    /**
     * Something the traveller asked for did not happen, and they need to be told.
     *
     * The last of the app's writing screens to get one — `LLM.md` §11 row #26. Discovery was
     * the exception because neither of its two arrangements had a snackbar host, so the
     * message had nowhere to land; both now have one.
     *
     * **There is deliberately no `error` field on [DiscoveryState] beside this.** Nothing on
     * this page renders a failure inline, so a field written on every failure and read by
     * nobody is exactly what turned a failed day summary into a spinner that just stopped —
     * §11 row #15. The route resolves the text with `userMessage()` at the moment it handles
     * the effect; resolving from state instead depends on a recomposition that has not
     * happened yet, which is how the message used to be lost.
     *
     * Every one of this screen's four writes now raises it. That is a wider fix than row #26
     * asked for and it is the same defect four times: `saveNote`, `deleteNote`,
     * `toggleFavorite` and `deleteDiscovery` all return `AppResult` and all four results were
     * being discarded, so a *handled* failure — the ordinary path, not a throw — took the
     * success branch. The note was the worst of the four because that branch closes the
     * composer, which threw away the traveller's own writing in the one place it existed.
     */
    data class ShowMessage(val error: AppError) : DiscoveryEffect

    /**
     * The objection is on disk; hand it to the traveller to send.
     *
     * An effect rather than a state flag, and it is the clearest case of the rule: the share
     * sheet must open exactly once per filed report. Held in state it would reopen on every
     * rotation, and on the frame after the sheet was dismissed there would be no honest value
     * to reset the flag to.
     *
     * It carries [discovery] whole rather than letting the screen read `state.discovery` when
     * it handles this. That is the same trap as `§11 row #15`, one step removed: the effect is
     * handled a main-queue turn after `sendEffect`, and a delete or a favourite landing in that
     * gap would compose the mail against a record the traveller was no longer looking at when
     * they pressed send. Everything the message quotes — the title, the model, the confidence,
     * the capture and its photograph — comes from this one object.
     */
    data class SendReport(
        val report: DiscoveryReport,
        val discovery: Discovery,
    ) : DiscoveryEffect
}
