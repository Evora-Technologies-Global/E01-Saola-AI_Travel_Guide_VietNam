package com.duylt.trave.vietlensai.performance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.duylt.trave.vietlensai.core.designsystem.theme.VietLensTheme
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.feature.camera.LensState
import com.duylt.trave.vietlensai.feature.chat.ChatState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import kotlin.time.Instant

/**
 * Recomposition counted on a real device, rather than inferred from a compiler report.
 *
 * [ComposeStabilityReportTest] proves the compiler *can* skip these composables. This proves
 * the runtime actually does — the two are different claims, and only the second is what the
 * traveller feels. A composable can be marked skippable and still recompose on every frame if
 * the caller hands it a fresh instance each time, which is precisely the mistake the stability
 * work was undoing.
 *
 * The measurement is a counter incremented in the body of a probe composable. Compose calls
 * that body exactly when it decides not to skip, so the count *is* the number of
 * recompositions — no sampling, no timing, nothing that can flake on a busy device.
 *
 * Runs as a device test because `LensState` and the rest are `internal` to `:shared`, so only
 * a test compiled inside the module can name them.
 */
class RecompositionTest {

    @get:Rule
    val compose = createComposeRule()

    /** One mutable int, deliberately not Compose state: writing it must not invalidate anything. */
    private class Counter {
        var value = 0
    }

    /**
     * A composable whose only job is to report that it ran.
     *
     * `state` is a real parameter of the real type, so this measures exactly what a screen
     * measures: whether Compose considers that type comparable by value.
     */
    @Composable
    private fun LensProbe(state: LensState, counter: Counter) {
        counter.value++
        Text(state.mode.name)
    }

    @Composable
    private fun ChatProbe(state: ChatState, counter: Counter) {
        counter.value++
        Text(state.input)
    }

    // ---------------------------------------------------------------------------------

    /**
     * An unrelated recomposition of the parent must not reach a child whose arguments held.
     *
     * This is the property the whole stability configuration buys. Before it, `LensState` was
     * unstable — because `:domain` is compiled without the Compose plugin, so `Discovery` and
     * `AppError` could not be proven immutable — and Compose fell back to comparing the state
     * object by reference. The reference was equal here, so this particular case would have
     * passed anyway; what would not is [a new state instance with equal contents does not
     * recompose the child], below.
     */
    @Test
    fun anUnrelatedParentRecompositionDoesNotReachTheChild() {
        val counter = Counter()
        var tick by mutableIntStateOf(0)

        compose.setContent {
            VietLensTheme {
                Column {
                    Text("tick $tick")
                    LensProbe(state = remember { LensState() }, counter = counter)
                }
            }
        }

        compose.waitForIdle()
        val afterFirstFrame = counter.value

        repeat(10) {
            tick++
            compose.waitForIdle()
        }

        assertEquals(
            "the child recomposed on a parent change that did not touch its arguments",
            afterFirstFrame,
            counter.value,
        )
    }

    /**
     * A *new* state instance holding equal values must not recompose the child.
     *
     * This is the one that was failing in production, invisibly. Every `setState { copy(...) }`
     * builds a new object, so the reference always differs; only a type Compose can compare
     * by `equals` gets skipped. With `LensState` unstable, every emission — the self-timer
     * once a second, the analysis stage ticker every 1.8 seconds — re-executed the entire
     * viewfinder tree beneath it.
     */
    @Test
    fun aNewStateInstanceWithEqualContentsDoesNotRecomposeTheChild() {
        val counter = Counter()
        var state by mutableStateOf(LensState())

        compose.setContent {
            VietLensTheme { LensProbe(state = state, counter = counter) }
        }

        compose.waitForIdle()
        val afterFirstFrame = counter.value

        // `copy()` with no arguments: a different instance, an equal value.
        repeat(10) {
            state = state.copy()
            compose.waitForIdle()
        }

        assertEquals(
            "a value-equal state instance still forced a recomposition — the state class is " +
                "unstable again. Check shared/compose-stability.conf and the report at " +
                "shared/build/compose-reports/shared-classes.txt.",
            afterFirstFrame,
            counter.value,
        )
    }

    /** The same guarantee for a state carrying a `List` of domain objects. */
    @Test
    fun aStateHoldingDomainListsIsComparedByValue() {
        val counter = Counter()
        val discoveries = List(20) { discovery("d$it") }
        var state by mutableStateOf(LensState(recentDiscoveries = discoveries))

        compose.setContent {
            VietLensTheme { LensProbe(state = state, counter = counter) }
        }

        compose.waitForIdle()
        val afterFirstFrame = counter.value

        repeat(10) {
            // A new list instance with the same contents, which is what a Room Flow emits
            // whenever any row in the table changes.
            state = state.copy(recentDiscoveries = discoveries.toList())
            compose.waitForIdle()
        }

        assertEquals(
            "an equal-but-not-identical List forced a recomposition; kotlin.collections.List " +
                "must be declared stable in shared/compose-stability.conf",
            afterFirstFrame,
            counter.value,
        )
    }

    /** And for the chat state, whose messages list grows on every exchange. */
    @Test
    fun theChatStateIsComparedByValue() {
        val counter = Counter()
        var state by mutableStateOf(ChatState())

        compose.setContent {
            VietLensTheme { ChatProbe(state = state, counter = counter) }
        }

        compose.waitForIdle()
        val afterFirstFrame = counter.value

        repeat(10) {
            state = state.copy()
            compose.waitForIdle()
        }

        assertEquals(afterFirstFrame, counter.value)
    }

    /**
     * A change that genuinely matters must still get through.
     *
     * Without this the three tests above are satisfied by a child that never recomposes at
     * all, which is a far worse bug than recomposing too often — the screen would simply stop
     * updating. Skipping is only correct if it is *conditional*.
     */
    @Test
    fun aRealChangeStillRecomposesTheChild() {
        val counter = Counter()
        var state by mutableStateOf(LensState())

        compose.setContent {
            VietLensTheme { LensProbe(state = state, counter = counter) }
        }

        compose.waitForIdle()
        val afterFirstFrame = counter.value

        state = state.copy(mode = LensMode.FOOD)
        compose.waitForIdle()

        assertEquals(
            "a changed field did not reach the child — it is being skipped when it should not be",
            afterFirstFrame + 1,
            counter.value,
        )
    }

    /**
     * A keyed row keeps its own composition state when something is inserted above it.
     *
     * What `key = { it.id }` buys is *slot identity*, not a skipped recomposition — the item
     * lambda closes over the list, so a new list re-invokes it either way. The difference is
     * what happens to everything the row `remember`ed: with a key, Compose moves the existing
     * slot and the state comes with it; without one, rows are identified by position, so
     * prepending shifts every row onto its neighbour's slot and each one silently adopts the
     * wrong state.
     *
     * That is worth a test rather than a code review because it is invisible until it is not:
     * an expanded card collapses, a half-typed note jumps to a different discovery, a loaded
     * image flashes onto the wrong row. The journal, the chat and the passport all prepend —
     * newest-first is the natural order for every one of them.
     */
    @Test
    fun aKeyedRowKeepsItsOwnStateWhenAnItemIsInsertedAbove() {
        var discoveries by mutableStateOf(List(5) { discovery("d$it") })
        // Each row stamps its own id into a `remember` on first composition. If the slot is
        // reused for a different row, the stamp and the row disagree.
        val stampMismatches = mutableListOf<String>()

        compose.setContent {
            VietLensTheme {
                LazyColumn {
                    items(discoveries, key = { it.id }) { item ->
                        val stampedId = remember { item.id }
                        if (stampedId != item.id) {
                            stampMismatches += "slot remembered $stampedId but now renders ${item.id}"
                        }
                        Text(item.title)
                    }
                }
            }
        }
        compose.waitForIdle()

        discoveries = listOf(discovery("new")) + discoveries
        compose.waitForIdle()

        assertEquals(
            "a row was rendered into a slot that belongs to a different row — the LazyColumn " +
                "lost its `key`, so remembered state now follows position instead of identity: " +
                "$stampMismatches",
            emptyList<String>(),
            stampMismatches,
        )
    }

    /**
     * The same list without a key, to prove the assertion above can fail.
     *
     * Without this, `aKeyedRowKeepsItsOwnStateWhenAnItemIsInsertedAbove` would pass just as
     * happily against a Compose version that had stopped honouring keys entirely — it only
     * ever asserts an empty list. This is the control: unkeyed rows *must* mis-adopt state,
     * which is what makes the keyed case a real result.
     */
    @Test
    fun anUnkeyedRowAdoptsTheWrongStateWhenAnItemIsInsertedAbove() {
        var discoveries by mutableStateOf(List(5) { discovery("d$it") })
        val stampMismatches = mutableListOf<String>()

        compose.setContent {
            VietLensTheme {
                LazyColumn {
                    items(discoveries) { item ->
                        val stampedId = remember { item.id }
                        if (stampedId != item.id) stampMismatches += item.id
                        Text(item.title)
                    }
                }
            }
        }
        compose.waitForIdle()

        discoveries = listOf(discovery("new")) + discoveries
        compose.waitForIdle()

        assertEquals(
            "an unkeyed LazyColumn preserved per-row state across an insertion. That is not " +
                "how Compose identifies unkeyed items, so the keyed test above is no longer " +
                "proving anything and both need rewriting.",
            true,
            stampMismatches.isNotEmpty(),
        )
    }

    private fun discovery(id: String) = Discovery(
        id = id,
        title = "Temple $id",
        localName = null,
        category = DiscoveryCategory.LANDMARK,
        imagePath = null,
        summary = "",
        sections = emptyList(),
        funFacts = emptyList(),
        tags = emptyList(),
        nearbySuggestions = emptyList(),
        suggestedQuestions = emptyList(),
        confidence = 0.9f,
        location = GeoPoint(21.0, 105.0),
        placeHint = null,
        isFavorite = false,
        modelUsed = null,
        createdAt = Instant.fromEpochSeconds(0),
    )
}
