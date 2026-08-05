package com.evora.technologies.saola.feature.collection

import com.evora.technologies.saola.domain.model.CultureCollection
import com.evora.technologies.saola.domain.usecase.ObserveCollectionUseCase
import com.evora.technologies.saola.testing.FakeCatalogRepository
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
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * The culture board, which has no request to make and therefore no failure to render.
 *
 * Small enough that the whole suite is about one thing: the spinner comes down on the first
 * emission even when the catalogue is empty. That is not a formality — an asset that failed
 * to load emits [CultureCollection.EMPTY], which is indistinguishable from a full board
 * nobody has collected from yet, and a screen that treated "nothing yet" as "still loading"
 * would spin for ever with no request in flight to explain it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {

    private val catalog = FakeCatalogRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `an empty catalogue still takes the spinner down`() = runTest {
        val vm = viewModel()
        runCurrent()

        assertFalse(vm.state.value.isLoading)
        assertEquals(0, vm.state.value.collection.total)
    }

    @Test
    fun `selecting and dismissing an item is the whole of the interaction`() = runTest {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(CollectionIntent.Select("ao-dai"))
        assertEquals("ao-dai", vm.state.value.selectedItemId)

        vm.onIntent(CollectionIntent.DismissSelection)
        assertNull(vm.state.value.selectedItemId)
    }

    @Test
    fun `an id with nothing behind it resolves to no entry rather than throwing`() = runTest {
        val vm = viewModel()
        runCurrent()

        vm.onIntent(CollectionIntent.Select("nothing-here"))

        assertNull(vm.state.value.selected, "a stale id is a missing sheet and not a crash")
    }

    private fun viewModel() = CollectionViewModel(
        observeCollection = ObserveCollectionUseCase(catalog),
    )
}
