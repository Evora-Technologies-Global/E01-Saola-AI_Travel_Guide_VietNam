package com.duylt.trave.vietlensai.feature.chat

import androidx.lifecycle.SavedStateHandle
import com.duylt.trave.vietlensai.domain.usecase.AskFollowUpUseCase
import com.duylt.trave.vietlensai.domain.usecase.ClearChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveChatUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveDiscoveryUseCase
import com.duylt.trave.vietlensai.domain.usecase.ObserveSettingsUseCase
import com.duylt.trave.vietlensai.navigation.Routes
import com.duylt.trave.vietlensai.testing.FakeChatRepository
import com.duylt.trave.vietlensai.testing.FakeDiscoveryRepository
import com.duylt.trave.vietlensai.testing.FakeSettingsRepository
import com.duylt.trave.vietlensai.testing.FakeTextToSpeech
import com.duylt.trave.vietlensai.testing.discovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Which discovery the guide is talking about.
 *
 * That is the whole subject of this class, and it is worth its own suite because getting it
 * wrong is silent: nothing throws, nothing logs, the thread simply belongs to another place
 * and the traveller reads about the One Pillar Pagoda while asking about the Temple of
 * Literature. The phone cannot reach that state — its chat has a route of its own carrying the
 * id — but the large-window arrangement composes the guide inside the *discovery's* entry and
 * names the discovery outright, which is a second way in and therefore a second way to be
 * wrong. See `ChatViewModel`'s KDoc.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val chat = FakeChatRepository()
    private val discoveries = FakeDiscoveryRepository()
    private val settings = FakeSettingsRepository()
    private val speech = FakeTextToSpeech()

    @BeforeTest
    fun setUp() {
        // viewModelScope is hard-wired to Dispatchers.Main.immediate, which does not exist on
        // a plain JVM runtime. Unconfined so the observers started in `init` have run by the
        // time the constructor returns — which is what the screen sees too.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        discoveries.discoveries.value = listOf(discovery(id = "d1"), discovery(id = "d2"))
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the route argument names the thread when no id is passed`() = runTest {
        val vm = viewModel(routeId = "d1")
        runCurrent()

        vm.onIntent(ChatIntent.SubmitQuestion("Who built it?"))
        runCurrent()

        assertEquals(listOf("d1" to "Who built it?"), chat.askedFor)
    }

    @Test
    fun `an explicit id wins over the route argument`() = runTest {
        // The pane the guide column lives in is the *discovery's* entry, whose argument happens
        // to carry the same name — so a fallback that quietly won would look correct in every
        // test that used matching values. Mismatched on purpose: only an explicit id winning
        // makes this pass.
        val vm = viewModel(routeId = "d1", explicitId = "d2")
        runCurrent()

        vm.onIntent(ChatIntent.SubmitQuestion("Who built it?"))
        runCurrent()

        assertEquals(listOf("d2" to "Who built it?"), chat.askedFor)
        assertEquals("d2", vm.state.value.discovery?.id)
    }

    @Test
    fun `two guides opened in turn read two different threads`() = runTest {
        val first = viewModel(routeId = "d1", explicitId = "d1")
        first.onIntent(ChatIntent.SubmitQuestion("About the first"))
        runCurrent()

        // What a second discovery opening in the same window must produce: its own ViewModel,
        // its own thread. Sharing one would show the traveller the previous place's answers.
        val second = viewModel(routeId = "d1", explicitId = "d2")
        second.onIntent(ChatIntent.SubmitQuestion("About the second"))
        runCurrent()

        assertEquals(
            listOf("d1" to "About the first", "d2" to "About the second"),
            chat.askedFor,
        )
        assertTrue(second.state.value.messages.all { it.discoveryId == "d2" })
        assertTrue(first.state.value.messages.all { it.discoveryId == "d1" })
    }

    @Test
    fun `clearing the thread clears only this discovery's`() = runTest {
        val vm = viewModel(routeId = "d1", explicitId = "d2")
        runCurrent()

        vm.onIntent(ChatIntent.ClearThread)
        runCurrent()

        assertEquals(listOf("d2"), chat.clearedThreads)
    }

    @Test
    fun `a repository that throws is contained rather than fatal`() = runTest {
        chat.throwOnAsk = IllegalStateException("Room is corrupt")
        val vm = viewModel(routeId = "d1")
        runCurrent()

        vm.onIntent(ChatIntent.SubmitQuestion("Who built it?"))
        runCurrent()

        assertTrue(vm.state.value.error != null, "an unwrapped throw must land in state")
        assertTrue(!vm.state.value.isSending, "the thinking card must come down")
    }

    private fun viewModel(routeId: String, explicitId: String? = null) = ChatViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_DISCOVERY_ID to routeId)),
        explicitDiscoveryId = explicitId,
        observeDiscovery = ObserveDiscoveryUseCase(discoveries),
        observeChat = ObserveChatUseCase(chat),
        observeSettings = ObserveSettingsUseCase(settings),
        askFollowUp = AskFollowUpUseCase(chat),
        clearChat = ClearChatUseCase(chat),
        textToSpeech = speech,
    )
}
