package com.evora.technologies.saola.feature.discovery

import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryNote
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
) : UiState {
    val isEditingNote: Boolean get() = noteEditor != null
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
}
