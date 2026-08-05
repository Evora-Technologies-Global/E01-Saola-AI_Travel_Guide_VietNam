package com.evora.technologies.saola.feature.discovery

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.domain.usecase.DeleteDiscoveryUseCase
import com.evora.technologies.saola.domain.usecase.DeleteNoteUseCase
import com.evora.technologies.saola.domain.usecase.ObserveDiscoveryUseCase
import com.evora.technologies.saola.domain.usecase.ObserveNoteUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.SaveNoteUseCase
import com.evora.technologies.saola.domain.usecase.ToggleFavoriteUseCase
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.navigation.Routes
import com.evora.technologies.saola.testing.FakeCaptureStore
import com.evora.technologies.saola.testing.FakeDiscoveryRepository
import com.evora.technologies.saola.testing.FakeNoteRepository
import com.evora.technologies.saola.testing.FakeSettingsRepository
import com.evora.technologies.saola.testing.FakeTextToSpeech
import com.evora.technologies.saola.testing.clearAsFrameworkWould
import com.evora.technologies.saola.testing.discovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The note composer, which is the only place in the app holding something the traveller wrote.
 *
 * Two properties are worth a suite of their own. The first is that a photo picked into an
 * abandoned note is deleted — it is copied into app storage the moment it is chosen, so every
 * way out of the composer is a way to leak a file that no row will ever name. The second is
 * that saving can be re-attempted: the screen raises a flag before the write which both
 * disables the button and guards re-entry, so a write that fails without lowering it costs the
 * traveller their words with no way to try again and nothing on screen to say why.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

    private val discoveries = FakeDiscoveryRepository()
    private val notes = FakeNoteRepository()
    private val settings = FakeSettingsRepository()
    private val captures = FakeCaptureStore()
    private val speech = FakeTextToSpeech()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        discoveries.discoveries.value = listOf(discovery(id = "d1"))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the discovery named by the route is the one rendered`() = runTest {
        val vm = viewModel()
        runCurrent()

        assertEquals("d1", vm.state.value.discovery?.id)
        assertFalse(vm.state.value.isLoading)
    }

    @Test
    fun `deleting navigates back rather than raising a flag in state`() = runTest {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(DiscoveryIntent.RequestDelete)
        assertTrue(vm.state.value.showDeleteConfirm)

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.ConfirmDelete)
            runCurrent()
            assertTrue(awaitItem() is DiscoveryEffect.NavigateBack)
        }
        assertFalse(vm.state.value.showDeleteConfirm)
    }

    @Test
    fun `a suggested question opens the guide with it already typed`() = runTest {
        val vm = viewModel()
        runCurrent()

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.AskSuggested("Who built it?"))
            runCurrent()
            val effect = awaitItem()
            assertTrue(effect is DiscoveryEffect.OpenChat)
            assertEquals("d1", effect.discoveryId)
            assertEquals("Who built it?", effect.prefill)
        }
        assertEquals(1, speech.stopCalls, "narration stops before the guide starts talking")
    }

    @Test
    fun `the composer opens on what was already saved`() = runTest {
        notes.save("d1", "Went at dawn", listOf("/captures/a.jpg"))
        val vm = viewModel()
        runCurrent()

        vm.onIntent(DiscoveryIntent.StartEditNote)

        assertEquals("Went at dawn", vm.state.value.noteEditor?.body)
        assertContentEquals(listOf("/captures/a.jpg"), vm.state.value.noteEditor?.photoPaths)
    }

    @Test
    fun `photos beyond the cap are deleted rather than left on disk`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)

        val tooMany = List(DiscoveryNote.MAX_PHOTOS + 2) { "/captures/p$it.jpg" }
        vm.onIntent(DiscoveryIntent.NotePhotoPicked(tooMany))
        runCurrent()

        assertEquals(DiscoveryNote.MAX_PHOTOS, vm.state.value.noteEditor?.photoPaths?.size)
        assertContentEquals(
            tooMany.takeLast(2),
            captures.deleted.filterNotNull(),
            "an imported file no note references is litter",
        )
    }

    @Test
    fun `abandoning an edit deletes the photos it had picked`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.NotePhotoPicked(listOf("/captures/new.jpg")))
        runCurrent()

        vm.onIntent(DiscoveryIntent.CancelEditNote)
        runCurrent()

        assertContentEquals(listOf("/captures/new.jpg"), captures.deleted.filterNotNull())
        assertNull(vm.state.value.noteEditor)
    }

    @Test
    fun `abandoning an edit leaves the photos the saved note still names`() = runTest {
        notes.save("d1", "Went at dawn", listOf("/captures/kept.jpg"))
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)

        vm.onIntent(DiscoveryIntent.CancelEditNote)
        runCurrent()

        assertTrue(captures.deleted.isEmpty(), "a saved photo survives a cancelled edit")
    }

    @Test
    fun `closing the screen mid-edit still clears up the picked photos`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.NotePhotoPicked(listOf("/captures/orphan.jpg")))
        runCurrent()

        // The one way out with no button behind it, and therefore the one nobody writes.
        vm.clearAsFrameworkWould()
        runCurrent()

        assertContentEquals(listOf("/captures/orphan.jpg"), captures.deleted.filterNotNull())
    }

    @Test
    fun `a photo arriving after the composer closed is not left behind`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.CancelEditNote)
        runCurrent()

        // The picker was still open when the composer went; the file already exists.
        vm.onIntent(DiscoveryIntent.NotePhotoPicked(listOf("/captures/late.jpg")))
        runCurrent()

        assertContentEquals(listOf("/captures/late.jpg"), captures.deleted.filterNotNull())
    }

    @Test
    fun `a save that throws leaves the note savable again`() = runTest {
        // `launchSafely` here takes the default onError — logging alone — so an unwrapped throw
        // is not merely unreported, it strands `isSavingNote`. `SaveNote` returns early while
        // that flag is up, so the composer keeps the traveller's words with the save button
        // dead and no message anywhere on screen. Nothing in the app can lower it again.
        notes.throwOnSave = IllegalStateException("Room is corrupt")
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.NoteBodyChanged("Went at dawn"))

        vm.onIntent(DiscoveryIntent.SaveNote)
        runCurrent()

        assertFalse(vm.state.value.isSavingNote, "the save button must come back")
        assertNotNull(vm.state.value.noteEditor, "the traveller's words must still be there")

        // The proof that it matters: a second attempt has to reach the repository.
        notes.throwOnSave = null
        vm.onIntent(DiscoveryIntent.SaveNote)
        runCurrent()

        assertEquals(2, notes.saved.size, "a retry after a throw must reach the repository")
        assertNull(vm.state.value.noteEditor, "and this time the composer closes")
    }

    @Test
    fun `a favourite that throws is contained rather than fatal`() = runTest {
        discoveries.throwOnToggleFavorite = IllegalStateException("Room is corrupt")
        val vm = viewModel()
        runCurrent()

        vm.onIntent(DiscoveryIntent.ToggleFavorite)
        runCurrent()

        assertNotNull(vm.state.value.discovery, "the page is still readable")
    }

    // ---- Every write reports, on both of its failure paths — `LLM.md` §11 row #26 ----
    //
    // All four of this screen's writes return `AppResult` and all four results were being
    // discarded, so the *handled* failure — the ordinary one, not a throw — silently took the
    // success branch. `failOn…` on the fakes is what reaches that path; `throwOn…` reaches
    // the other. Both are checked for each write, because they are separate code and the
    // existing suite only ever exercised the throw.

    @Test
    fun `a note that fails to save keeps the words and says why`() = runTest {
        notes.failOnSave = AppError.Storage("disk full")
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.NoteBodyChanged("Went at dawn"))

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.SaveNote)
            runCurrent()
            val effect = awaitItem()
            assertTrue(effect is DiscoveryEffect.ShowMessage, "the traveller has to be told")
            assertEquals(AppError.Storage("disk full"), effect.error)
        }

        // The regression this exists for: the composer used to close one statement after the
        // discarded result, which threw the writing away in the only place it existed.
        assertEquals(
            "Went at dawn",
            vm.state.value.noteEditor?.body,
            "the words survive a failed save",
        )
        assertFalse(vm.state.value.isSavingNote, "and the save button comes back")

        // And the flag really is lowered rather than merely re-read: a retry reaches storage.
        notes.failOnSave = null
        vm.onIntent(DiscoveryIntent.SaveNote)
        runCurrent()
        assertEquals(2, notes.saved.size)
        assertNull(vm.state.value.noteEditor)
    }

    @Test
    fun `a note that throws while saving also reports`() = runTest {
        notes.throwOnSave = IllegalStateException("Room is corrupt")
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)
        vm.onIntent(DiscoveryIntent.NoteBodyChanged("Went at dawn"))

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.SaveNote)
            runCurrent()
            assertTrue(awaitItem() is DiscoveryEffect.ShowMessage)
        }
        assertNotNull(vm.state.value.noteEditor)
    }

    @Test
    fun `a delete that fails does not take the traveller back to a discovery still there`() =
        runTest {
            discoveries.failOnDelete = AppError.Storage("disk full")
            val vm = viewModel()
            runCurrent()
            vm.onIntent(DiscoveryIntent.RequestDelete)

            vm.effects.test {
                vm.onIntent(DiscoveryIntent.ConfirmDelete)
                runCurrent()
                // The whole point: a message, and *not* a NavigateBack. Leaving was
                // unconditional, so a failed delete dropped the traveller into a journal that
                // still listed the discovery — which reads as a stale list, not as a failure.
                assertTrue(awaitItem() is DiscoveryEffect.ShowMessage)
                expectNoEvents()
            }
            assertNotNull(vm.state.value.discovery, "the page is still the one they were on")
        }

    @Test
    fun `a delete that succeeds still navigates back exactly once`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.RequestDelete)

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.ConfirmDelete)
            runCurrent()
            assertTrue(awaitItem() is DiscoveryEffect.NavigateBack)
            expectNoEvents()
        }
        assertContentEquals(listOf("d1"), discoveries.deleted)
    }

    @Test
    fun `a note that fails to delete keeps the note and says why`() = runTest {
        notes.save("d1", "Went at dawn", emptyList())
        notes.failOnDelete = AppError.Storage("disk full")
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.StartEditNote)

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.DeleteNote)
            runCurrent()
            assertTrue(awaitItem() is DiscoveryEffect.ShowMessage)
        }
        assertNotNull(vm.state.value.noteEditor, "the composer stays open on a failed delete")
    }

    @Test
    fun `a favourite that fails to stick says so`() = runTest {
        discoveries.failOnToggleFavorite = AppError.Storage("disk full")
        val vm = viewModel()
        runCurrent()

        vm.effects.test {
            vm.onIntent(DiscoveryIntent.ToggleFavorite)
            runCurrent()
            assertTrue(awaitItem() is DiscoveryEffect.ShowMessage)
        }
    }

    @Test
    fun `leaving silences only the narration this screen started`() = runTest {
        val vm = viewModel()
        runCurrent()
        vm.onIntent(DiscoveryIntent.ToggleSpeech)
        runCurrent()
        assertEquals(1, speech.spoken.size)

        vm.clearAsFrameworkWould()

        assertEquals(1, speech.stopCalls, "its own narration is stopped")
    }

    @Test
    fun `leaving lets another screen's narration keep playing`() = runTest {
        val vm = viewModel()
        runCurrent()
        // Something else is talking — a chat reply started over the top of this page.
        speech.speak("An answer", bcp47 = "en-US", utteranceId = "chat-1")

        vm.clearAsFrameworkWould()

        assertEquals(0, speech.stopCalls, "a reply playing over the top keeps going")
    }

    @Test
    fun `every intent in any order is survivable`() = runTest {
        val everyIntent = listOf(
            DiscoveryIntent.ToggleFavorite,
            DiscoveryIntent.ToggleSpeech,
            DiscoveryIntent.RequestDelete,
            DiscoveryIntent.CancelDelete,
            DiscoveryIntent.AskSuggested("Who built it?"),
            DiscoveryIntent.StartEditNote,
            DiscoveryIntent.NoteBodyChanged("Went at dawn"),
            DiscoveryIntent.NotePhotoPicked(listOf("/captures/a.jpg", "/captures/b.jpg")),
            DiscoveryIntent.NotePhotoCaptured("/captures/c.jpg"),
            DiscoveryIntent.NotePhotoRemoved("/captures/a.jpg"),
            DiscoveryIntent.SaveNote,
            DiscoveryIntent.DeleteNote,
            DiscoveryIntent.CancelEditNote,
        )

        val random = Random(seed = 20260804)
        repeat(times = 25) {
            val vm = viewModel()
            everyIntent.shuffled(random).forEach { intent ->
                vm.onIntent(intent)
                runCurrent()
            }
            vm.clearAsFrameworkWould()
            runCurrent()
        }
    }

    private fun TestScope.viewModel() = DiscoveryViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_DISCOVERY_ID to "d1")),
        observeDiscovery = ObserveDiscoveryUseCase(discoveries),
        observeSettings = ObserveSettingsUseCase(settings),
        observeNote = ObserveNoteUseCase(notes),
        toggleFavorite = ToggleFavoriteUseCase(discoveries),
        deleteDiscovery = DeleteDiscoveryUseCase(discoveries),
        saveNote = SaveNoteUseCase(notes),
        deleteNote = DeleteNoteUseCase(notes),
        captureStore = captures,
        // The real one outlives the ViewModel on purpose — the file clean-up runs at exactly
        // the moments this scope is being cleared. `backgroundScope` is the test's equivalent:
        // it survives the ViewModel and is torn down with the test rather than with the screen.
        applicationScope = backgroundScope,
        textToSpeech = speech,
    )
}
