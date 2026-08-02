package com.duylt.trave.vietlensai.feature.discovery

import com.duylt.trave.vietlensai.core.mvi.UiEffect
import com.duylt.trave.vietlensai.core.mvi.UiIntent
import com.duylt.trave.vietlensai.core.mvi.UiState
import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote

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
}
