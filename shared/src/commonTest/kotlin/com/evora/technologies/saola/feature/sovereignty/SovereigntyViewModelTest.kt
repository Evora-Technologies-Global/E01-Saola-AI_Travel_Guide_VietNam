package com.evora.technologies.saola.feature.sovereignty

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * The statement, whose words are in the string table and whose map is an asset.
 *
 * There is no repository here and nothing to fake — the figure is read through Compose
 * Resources, which a unit-test runtime has no bundle for. That is precisely the case worth
 * pinning: the screen's whole promise is that a map which cannot be read costs the *figure*
 * and never the statement, so a runtime with no asset at all is the strongest available form
 * of that test rather than a limitation of it. See the plan's phase 03 for why the dependency
 * is worth inverting anyway.
 *
 * **Real time, not virtual.** This is the one suite in the project that cannot use `runTest`.
 * `load()` hands its work to `Dispatchers.Default`, a real thread pool the test scheduler does
 * not drive, so `advanceTimeBy` returns long before the read has finished and the assertion
 * reads a state that is still loading. `runBlocking` with a real `withTimeout` waits for the
 * answer instead of pretending to, and the timeout is what keeps a read that never returns a
 * failure rather than a hung suite.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SovereigntyViewModelTest {

    @BeforeTest
    fun setUp() {
        // `Dispatchers.Unconfined` rather than a test dispatcher: there is no virtual clock in
        // this suite to attach one to, and `viewModelScope` only needs a Main that runs eagerly.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a map that cannot be read still lets the statement render`() = runBlocking {
        val vm = SovereigntyViewModel()

        val settled = withTimeout(SETTLE_TIMEOUT_MILLIS) {
            vm.state.first { !it.isLoading }
        }

        assertFalse(
            settled.isLoading,
            "the words are the screen and they do not wait on the figure",
        )
    }

    private companion object {
        /** Generous, because it covers a real 102 KB parse on a cold pool — not a poll interval. */
        const val SETTLE_TIMEOUT_MILLIS = 10_000L
    }
}
