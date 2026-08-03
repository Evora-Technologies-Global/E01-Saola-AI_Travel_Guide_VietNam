package com.duylt.trave.vietlensai.feature.discovery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.usecase.DeleteDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.DeleteNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.domain.usecase.SaveNoteUseCase
import com.duylt.trave.vietlensai.domain.usecase.ToggleFavoriteUseCase
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DiscoveryViewModel(
    savedStateHandle: SavedStateHandle,
    observeDiscovery: ObserveDiscoveryUseCase,
    observeSettings: ObserveSettingsUseCase,
    observeNote: ObserveNoteUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val deleteDiscovery: DeleteDiscoveryUseCase,
    private val saveNote: SaveNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val captureStore: CaptureStore,
    private val applicationScope: CoroutineScope,
    private val textToSpeech: TextToSpeechManager,
) : MviViewModel<DiscoveryState, DiscoveryIntent, DiscoveryEffect>(DiscoveryState()) {

    private val discoveryId: String = checkNotNull(savedStateHandle[Routes.ARG_DISCOVERY_ID])

    init {
        observeDiscovery(discoveryId)
            .onEach { discovery ->
                setState { copy(isLoading = false, discovery = discovery) }
            }
            .launchIn(viewModelScope)

        observeSettings()
            .onEach { settings -> setState { copy(language = settings.language) } }
            .launchIn(viewModelScope)

        observeNote(discoveryId)
            .onEach { note -> setState { copy(note = note) } }
            .launchIn(viewModelScope)

        // The speaking flag is owned by the engine, not by this screen: another
        // screen can stop playback, and the button here has to reflect that.
        textToSpeech.isSpeaking
            .onEach { speaking -> setState { copy(isSpeaking = speaking) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: DiscoveryIntent) {
        when (intent) {
            DiscoveryIntent.ToggleFavorite -> launchSafely { toggleFavorite(discoveryId) }

            DiscoveryIntent.ToggleSpeech -> {
                val discovery = currentState.discovery ?: return
                if (currentState.isSpeaking) {
                    textToSpeech.stop()
                } else {
                    textToSpeech.speak(discovery.toNarration(), currentState.language, discoveryId)
                }
            }

            DiscoveryIntent.RequestDelete -> setState { copy(showDeleteConfirm = true) }
            DiscoveryIntent.CancelDelete -> setState { copy(showDeleteConfirm = false) }

            DiscoveryIntent.ConfirmDelete -> launchSafely {
                setState { copy(showDeleteConfirm = false) }
                textToSpeech.stop()
                deleteDiscovery(discoveryId)
                sendEffect(DiscoveryEffect.NavigateBack)
            }

            is DiscoveryIntent.AskSuggested -> {
                textToSpeech.stop()
                sendEffect(DiscoveryEffect.OpenChat(discoveryId, intent.question))
            }

            DiscoveryIntent.StartEditNote -> setState {
                val saved = note
                copy(
                    noteEditor = NoteEditor(
                        body = saved?.body.orEmpty(),
                        photoPaths = saved?.photoPaths.orEmpty(),
                    ),
                )
            }

            DiscoveryIntent.CancelEditNote -> {
                // Photos picked during an abandoned edit were copied into app storage the
                // moment they were chosen, and backing out is the last chance anything has
                // to know they were never wanted.
                discardUnsaved(currentState.noteEditor?.photoPaths.orEmpty())
                setState { copy(noteEditor = null) }
            }

            is DiscoveryIntent.NoteBodyChanged -> setState {
                copy(noteEditor = noteEditor?.copy(body = intent.body))
            }

            is DiscoveryIntent.NotePhotoPicked -> acceptNotePhotos(intent.paths)

            is DiscoveryIntent.NotePhotoCaptured -> launchSafely {
                // Before it is shown, not after: the strip renders straight from the file,
                // and a capture whose rotation is still only an EXIF tag reads sideways in
                // any renderer that does not consult one.
                captureStore.flattenOrientation(intent.path)
                acceptNotePhotos(listOf(intent.path))
            }

            is DiscoveryIntent.NotePhotoRemoved -> {
                // A photo that was never in the saved note has nothing left pointing at it
                // once it leaves the composer. One already saved is left alone until the
                // edit is committed, so cancelling still brings it back.
                discardUnsaved(listOf(intent.path))
                setState {
                    copy(
                        noteEditor = noteEditor?.let { editor ->
                            editor.copy(photoPaths = editor.photoPaths - intent.path)
                        },
                    )
                }
            }

            DiscoveryIntent.SaveNote -> {
                val editor = currentState.noteEditor ?: return
                if (currentState.isSavingNote) return
                setState { copy(isSavingNote = true) }
                launchSafely {
                    saveNote(discoveryId, editor.body, editor.photoPaths)
                    setState { copy(isSavingNote = false, noteEditor = null) }
                }
            }

            DiscoveryIntent.DeleteNote -> launchSafely {
                deleteNote(discoveryId)
                setState { copy(noteEditor = null) }
            }
        }
    }

    /**
     * Takes what fits into the composer and deletes the rest.
     *
     * Shared by the picker and the camera so the cap is enforced in exactly one place —
     * and so neither route can leave a file behind that no note will ever reference.
     */
    private fun acceptNotePhotos(paths: List<String>) {
        val editor = currentState.noteEditor
        if (editor == null) {
            // The composer closed while the picker or camera was still open. The files are
            // already in app storage and nothing will ever point at them.
            discardUnsaved(paths)
            return
        }
        val room = DiscoveryNote.MAX_PHOTOS - editor.photoPaths.size
        val accepted = paths
            .filterNot { it in editor.photoPaths }
            .take(room)

        // The picker was told how many slots were left, so a surplus means it did not
        // honour the limit. Whatever did not fit is deleted rather than left behind — an
        // imported file no note references is litter by definition.
        discardUnsaved(paths - accepted.toSet())

        if (accepted.isEmpty()) return
        setState {
            copy(noteEditor = noteEditor?.let { it.copy(photoPaths = it.photoPaths + accepted) })
        }
    }

    /**
     * Deletes photo files the saved note does not reference; a no-op for the ones it does.
     *
     * Runs on the application scope, not [viewModelScope]: the two moments this matters
     * most — cancelling an edit and leaving the screen — are both moments where this
     * ViewModel is about to be cleared, and work launched on its own scope would be
     * cancelled before the first file was touched.
     */
    private fun discardUnsaved(paths: List<String>) {
        val saved = currentState.note?.photoPaths.orEmpty()
        val orphans = paths.filterNot { it in saved }
        if (orphans.isEmpty()) return
        applicationScope.launch { orphans.forEach { captureStore.delete(it) } }
    }

    override fun onCleared() {
        // Closing the screen mid-edit is a third way to abandon picked photos, alongside
        // cancelling and removing one. It is the only one with no button behind it, so it
        // has to be caught here or the files are leaked with nothing left to name them.
        discardUnsaved(currentState.noteEditor?.photoPaths.orEmpty())

        // Only silence narration this screen started; a chat reply playing over
        // the top should keep going while the user navigates back.
        if (textToSpeech.currentUtteranceId.value == discoveryId) {
            textToSpeech.stop()
        }
        super.onCleared()
    }
}

/**
 * Flattens a discovery into one continuous passage for the voice guide.
 *
 * Section titles are read as sentences ("History.") rather than skipped: without
 * them a five-minute narration turns into an undifferentiated wall of speech.
 */
private fun Discovery.toNarration(): String = buildString {
    append(title)
    if (!localName.isNullOrBlank() && localName != title) {
        append(", ").append(localName)
    }
    append(". ").append(summary)
    sections.forEach { section ->
        append(' ').append(section.title).append(". ").append(section.body)
    }
    if (funFacts.isNotEmpty()) {
        funFacts.forEach { fact -> append(' ').append(fact) }
    }
}
