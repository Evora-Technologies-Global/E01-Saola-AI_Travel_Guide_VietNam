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
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.onFailure
import com.duylt.trave.vietlensai.domain.util.onSuccess
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
            // The heart is drawn from the observed discovery rather than from a local flag,
            // so a refused write corrects itself on the next emission and the only thing
            // missing was the reason. `report` covers the throw path and `onFailure` the
            // handled one; both exist because a repository may do either.
            DiscoveryIntent.ToggleFavorite -> launchSafely(onError = ::report) {
                toggleFavorite(discoveryId).onFailure(::report)
            }

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

            // **Back only on success.** Leaving the page was unconditional, so a delete that
            // failed took the traveller to the journal with the discovery still in it —
            // which reads as a list that has not refreshed rather than as a failure, and is
            // the one wrong outcome here that nothing later corrects.
            DiscoveryIntent.ConfirmDelete -> launchSafely(onError = ::report) {
                setState { copy(showDeleteConfirm = false) }
                textToSpeech.stop()
                deleteDiscovery(discoveryId)
                    .onSuccess { sendEffect(DiscoveryEffect.NavigateBack) }
                    .onFailure(::report)
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
                // `isSavingNote = false` on the throw path too, and it is the flag rather
                // than the message that matters here. The guard two lines up reads the same
                // flag, so a write that raised it and died without lowering it left the
                // composer holding the traveller's own writing behind a save button that
                // had stopped doing anything — permanently, with nothing else in the app
                // able to lower it again. The editor is deliberately *not* cleared: closing
                // it would discard the words in the one place they exist.
                launchSafely(
                    onError = { error ->
                        setState { copy(isSavingNote = false) }
                        report(error)
                    },
                ) {
                    saveNote(discoveryId, editor.body, editor.photoPaths)
                        // The composer closes on this line and on no other. It used to close
                        // unconditionally, one statement after a call whose `AppResult` was
                        // discarded — so an ordinary handled failure looked exactly like a
                        // save, and the words went with the composer.
                        .onSuccess { setState { copy(isSavingNote = false, noteEditor = null) } }
                        .onFailure { error ->
                            setState { copy(isSavingNote = false) }
                            report(error)
                        }
                }
            }

            // Same shape one size down: the editor closes only if the row really went.
            DiscoveryIntent.DeleteNote -> launchSafely(onError = ::report) {
                deleteNote(discoveryId)
                    .onSuccess { setState { copy(noteEditor = null) } }
                    .onFailure(::report)
            }
        }
    }

    /**
     * Tells the traveller a write did not happen.
     *
     * One private method rather than the effect spelled out at eight call sites — four
     * writes, each with a handled-failure arm and an unwrapped-throw arm — because that is
     * eight chances for one of them to be forgotten, which is precisely how this screen came
     * to have four silent failures on it in the first place.
     *
     * It raises an effect and writes nothing to state; see the note on
     * [DiscoveryEffect.ShowMessage] for why there is no `error` field to write to.
     */
    private fun report(error: AppError) = sendEffect(DiscoveryEffect.ShowMessage(error))

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
