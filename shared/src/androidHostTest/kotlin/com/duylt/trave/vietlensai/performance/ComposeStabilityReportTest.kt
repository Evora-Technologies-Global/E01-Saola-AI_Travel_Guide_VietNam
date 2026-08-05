package com.duylt.trave.vietlensai.performance

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The recomposition gate.
 *
 * Wasted recomposition is invisible in a normal test: the screen still renders the right
 * pixels, the assertions still pass, and the only symptom is that the phone in someone's
 * hand feels slightly wrong. The Compose compiler already knows the answer, though — for
 * every composable it compiles it records whether the function can be *skipped* when its
 * arguments have not changed, and for every class whether it is *stable* enough to compare
 * by value. This test reads that record and holds it to a standard.
 *
 * Two properties are checked, and they fail for different reasons:
 *
 *  1. **Every restartable composable must be skippable.** A restartable-but-not-skippable
 *     composable re-executes its whole body every single time its parent recomposes, no
 *     matter what changed. There is no way to opt out at the call site — the only fix is at
 *     the declaration. Zero is the right number and the project is at zero, so any
 *     regression here is a genuine new defect rather than a threshold argument.
 *
 *  2. **No new composable may take an unstable parameter**, beyond a named list of ones
 *     that are unavoidable and harmless. An unstable parameter is compared by reference
 *     instead of by `equals`, so a composable holding one skips only while the caller keeps
 *     handing it the identical instance. That is true of a `remember`ed controller and
 *     false of anything rebuilt by `state.copy(...)` — which is why the allowlist below is
 *     entirely remembered singletons, and why a *state* class appearing here would be a
 *     real regression.
 *
 * Before `compose-stability.conf` existed, 82 of 191 composables took an unstable
 * parameter: every screen's state class was unstable, because `:domain` is compiled without
 * the Compose plugin and so none of its immutable data classes could be proven immutable.
 * Every `setState { copy(...) }` therefore re-executed the entire screen below it. The
 * lens screen does that once a second while a self-timer runs and every 1.8 seconds while
 * a recognition is in flight.
 *
 * @see ../../../../../../compose-stability.conf for what is declared stable and why.
 */
class ComposeStabilityReportTest {

    /**
     * Composables allowed to take a parameter Compose cannot prove stable.
     *
     * Every entry is a type that is genuinely not stable *and* is only ever passed as a
     * single remembered instance, which is the combination Compose's reference comparison
     * handles correctly:
     *
     *  - `…Route(viewModel)` — a ViewModel holds jobs and mutable fields, so it is unstable
     *    by construction. Koin hands the same instance back for the lifetime of the nav
     *    entry, and a Route composable's body is one call to its screen.
     *    Two Routes take more than one, and both rest on the same argument as the first: they
     *    are the large-window screens that show more than one feature at once, and every extra
     *    ViewModel is resolved from the same nav entry so Koin hands back the identical
     *    instance for as long as that screen is open. `DiscoveryTabletRoute` has the guide's
     *    `ChatViewModel` beside the discovery's, keyed on `discoveryId`.
     *    `JournalTabletRoute` has the passport's and the collection's beside the journal's,
     *    because on a large window those two are the pane next to the day column rather than
     *    screens of their own — see `tablet/feature/journal/JournalTabletScreen.kt`. Resolving
     *    them on the Route rather than inside each pane is what keeps §5 true: only a Route
     *    touches a ViewModel, and the panes below take `state` and `onIntent`.
     *  - `LensHost(viewModel)`, `ExploreHost(viewModel)` and `SettingsHost(viewModel)` — the
     *    same argument as a Route's, because each *is* its screen's Route: the half both
     *    arrangements share, lifted out of them so the capture coroutine, the map's permission
     *    bridge and the settings snackbar rule exist once rather than twice. See
     *    `feature/camera/LensHost.kt`, `feature/explore/ExploreHost.kt` and
     *    `feature/settings/SettingsHost.kt`.
     *  - `navController` — same shape, remembered once in `VietLensRoot` and passed down
     *    through whichever branch shell the window size selects.
     *  - `controller` — [com.duylt.trave.vietlensai.feature.camera.CameraController], which
     *    wraps the platform camera session and is `remember`ed for the screen's lifetime.
     *  - `model: Any?` on `AppAsyncImage` — Coil's model parameter is `Any?` by design.
     *  - `map` — the sovereignty outlines, `List<DoubleArray>`. Arrays are mutable, so
     *    unstable is the *correct* answer; the value is parsed from an asset once and
     *    remembered, never edited. It reaches five entries on 04.08.2026, when the statement
     *    was split into a Route and a stateless document so a large window could arrange it
     *    (`LLM.md` §11 row #13): the map is now threaded from the Route through the screen or
     *    the overlay into the document and its panel. Every one of them is the same instance
     *    the ViewModel parsed once, so the reference comparison is doing exactly what it does
     *    on the Route — and the whole chain changes value once in the screen's life.
     *  - `effects` / `onEffect` on [com.duylt.trave.vietlensai.core.mvi.CollectEffects] —
     *    `Flow` is an interface Compose cannot see inside, and a suspend lambda is never
     *    stable. Neither is a remembered singleton in the usual sense, and this entry rests
     *    on a different argument: the whole body of that composable is one `LaunchedEffect`
     *    keyed on `effects` and the lifecycle owner. `effects` is created once by
     *    `MviViewModel`, so re-executing the body restarts nothing — it re-reads two keys
     *    that did not change. The handler is deliberately *not* a key; it is held through
     *    `rememberUpdatedState` precisely so a new lambda each recomposition cannot tear the
     *    collector down and rebuild it.
     *
     * Adding to this list is a deliberate act. A state class turning up here means a screen
     * has stopped skipping, and the fix is `compose-stability.conf`, not this allowlist.
     */
    private val allowedUnstableParams: Map<String, Set<String>> = mapOf(
        "AppAsyncImage" to setOf("model"),
        "CollectEffects" to setOf("effects", "onEffect"),
        "CameraPreview" to setOf("controller"),
        "LensRoute" to setOf("viewModel"),
        "LensScreen" to setOf("controller"),
        "LensTabletRoute" to setOf("viewModel"),
        "LensTabletScreen" to setOf("controller"),
        "LensHost" to setOf("viewModel"),
        "ViewfinderPane" to setOf("controller"),
        "CameraFrame" to setOf("controller"),
        "Viewfinder" to setOf("controller"),
        "rememberZoomDriver" to setOf("controller"),
        "ChatRoute" to setOf("viewModel"),
        "CollectionRoute" to setOf("viewModel"),
        "DiscoveryRoute" to setOf("viewModel"),
        "DiscoveryTabletRoute" to setOf("viewModel", "chatViewModel"),
        "ExploreRoute" to setOf("viewModel"),
        "ExploreTabletRoute" to setOf("viewModel"),
        "ExploreHost" to setOf("viewModel"),
        "JournalRoute" to setOf("viewModel"),
        "JournalTabletRoute" to setOf("viewModel", "passportViewModel", "collectionViewModel"),
        "PassportRoute" to setOf("viewModel"),
        "SettingsRoute" to setOf("viewModel"),
        "SettingsTabletRoute" to setOf("viewModel"),
        "SettingsHost" to setOf("viewModel"),
        "SovereigntyRoute" to setOf("viewModel"),
        "SovereigntyTabletRoute" to setOf("viewModel"),
        "TranslationRoute" to setOf("viewModel"),
        "SovereigntyMapCanvas" to setOf("map"),
        "SovereigntyMapPanel" to setOf("map"),
        "SovereigntyScreen" to setOf("map"),
        "SovereigntyOverlay" to setOf("map"),
        "VietLensRoot" to setOf("navController"),
        "VietLensApp" to setOf("navController"),
        "VietLensTabletApp" to setOf("navController"),
        "VietLensNavHost" to setOf("navController"),
        "VietLensTabletNavHost" to setOf("navController"),
    )

    /**
     * State classes that must stay stable, named individually.
     *
     * These are the ones a screen passes wholesale to its content composable, so each is
     * the single point at which that screen either skips or does not. Checking them by name
     * rather than relying on the aggregate count means the failure message says *which*
     * screen regressed.
     *
     * `SovereigntyState` is the one screen state deliberately absent. It holds the parsed
     * `RegionMap`, whose outlines are `List<DoubleArray>` — arrays are mutable, so unstable is
     * the correct answer and no entry in `compose-stability.conf` could honestly say otherwise.
     * It costs nothing: the state is read inside `SovereigntyRoute` and never passed to a
     * composable as a whole, and it changes exactly once, when the asset finishes parsing.
     */
    private val stateClassesThatMustBeStable = listOf(
        "feature.camera.LensState",
        "feature.chat.ChatState",
        "feature.collection.CollectionState",
        "feature.discovery.DiscoveryState",
        "feature.explore.ExploreState",
        "feature.journal.JournalState",
        "feature.passport.PassportState",
        "feature.settings.SettingsState",
        "feature.translate.TranslationState",
    )

    @Test
    fun `every restartable composable is skippable`() {
        val offenders = composables()
            .filter { it.restartable && !it.skippable }
            .map { it.qualifiedName }

        assertTrue(
            offenders.isEmpty(),
            buildString {
                appendLine("${offenders.size} composable(s) are restartable but NOT skippable.")
                appendLine("Each one re-runs its entire body every time its parent recomposes,")
                appendLine("whatever changed. Usually the cause is a parameter whose type Compose")
                appendLine("cannot prove stable — declare it in shared/compose-stability.conf.")
                offenders.forEach { appendLine("  - $it") }
            },
        )
    }

    @Test
    fun `no composable takes an unstable parameter outside the allowlist`() {
        val unexpected = mutableListOf<String>()

        composablesWithUnstableParams().forEach { (name, params) ->
            val allowed = allowedUnstableParams[name].orEmpty()
            (params - allowed).forEach { param ->
                unexpected += "$name(… $param …)"
            }
        }

        assertTrue(
            unexpected.isEmpty(),
            buildString {
                appendLine("${unexpected.size} composable parameter(s) became unstable.")
                appendLine("An unstable parameter is compared by reference, so the composable")
                appendLine("can only skip while the caller passes the very same instance — which")
                appendLine("a `state.copy(...)` never does. Fix the type's stability in")
                appendLine("shared/compose-stability.conf rather than widening the allowlist,")
                appendLine("unless the value is a remembered singleton like a ViewModel.")
                unexpected.forEach { appendLine("  - $it") }
            },
        )
    }

    @Test
    fun `screen state classes are all stable`() {
        val classes = classesReport()
        val unstable = stateClassesThatMustBeStable.filter { suffix ->
            classes.contains("unstable class com.duylt.trave.vietlensai.$suffix ")
        }
        val missing = stateClassesThatMustBeStable.filterNot { suffix ->
            classes.contains("class com.duylt.trave.vietlensai.$suffix ")
        }

        assertTrue(
            missing.isEmpty(),
            "Not found in the Compose report — renamed or deleted? Update this test: $missing",
        )
        assertTrue(
            unstable.isEmpty(),
            buildString {
                appendLine("${unstable.size} screen state class(es) are unstable.")
                appendLine("The screen they belong to now recomposes in full on every state")
                appendLine("emission, including ones that changed a single unrelated field.")
                appendLine("Look at the class in shared/build/compose-reports/shared-classes.txt:")
                appendLine("the field marked `unstable` is the cause.")
                unstable.forEach { appendLine("  - $it") }
            },
        )
    }

    /**
     * A guard on the aggregate, so a change that makes many things worse at once is caught
     * even if each individual composable happens to stay inside the rules above.
     *
     * The ceiling is the count at the time the stability configuration went in, not a target
     * to grow into.
     */
    @Test
    fun `the number of unstable classes has not grown`() {
        val count = classesReport().lines().count { it.startsWith("unstable class ") }

        assertTrue(
            count <= UNSTABLE_CLASS_CEILING,
            "Unstable classes in :shared rose to $count, above the agreed ceiling of " +
                "$UNSTABLE_CLASS_CEILING (it was 38 before shared/compose-stability.conf). " +
                "See shared/build/compose-reports/shared-classes.txt.",
        )
    }

    // ---------------------------------------------------------------------------------
    // Reading the reports
    // ---------------------------------------------------------------------------------

    private data class ComposableEntry(
        val qualifiedName: String,
        val simpleName: String,
        val restartable: Boolean,
        val skippable: Boolean,
    )

    /** Parses `shared-composables.csv`, which the compiler writes one row per composable. */
    private fun composables(): List<ComposableEntry> {
        val lines = reportFile("shared-composables.csv").readLines().filter { it.isNotBlank() }
        val header = lines.first().split(',')
        val nameAt = header.indexOf("package")
        val simpleAt = header.indexOf("name")
        val skippableAt = header.indexOf("skippable")
        val restartableAt = header.indexOf("restartable")

        return lines.drop(1).mapNotNull { line ->
            val cells = line.split(',')
            if (cells.size <= maxOf(nameAt, simpleAt, skippableAt, restartableAt)) return@mapNotNull null
            ComposableEntry(
                qualifiedName = cells[nameAt],
                simpleName = cells[simpleAt],
                restartable = cells[restartableAt] == "1",
                skippable = cells[skippableAt] == "1",
            )
        }
    }

    /**
     * Composable simple name to the set of its parameters the compiler marked unstable.
     *
     * Read from the `.txt` report rather than the `.csv`, because only the text form breaks
     * a composable down into its parameters and their inferred stability. Its shape is:
     *
     * ```
     * restartable skippable fun ChatScreen(
     *   unstable state: ChatState
     *   stable onIntent: Function1<ChatIntent, Unit>
     * )
     * ```
     */
    private fun composablesWithUnstableParams(): Map<String, Set<String>> {
        val text = reportFile("shared-composables.txt").readText()
        val result = mutableMapOf<String, MutableSet<String>>()

        var current: String? = null
        text.lineSequence().forEach { line ->
            val declaration = DECLARATION.find(line)
            if (declaration != null) {
                current = declaration.groupValues[1].substringAfterLast('.')
                return@forEach
            }
            if (line.startsWith(")") || line.isBlank()) {
                current = null
                return@forEach
            }
            val name = current ?: return@forEach
            val param = UNSTABLE_PARAM.find(line) ?: return@forEach
            result.getOrPut(name) { mutableSetOf() } += param.groupValues[1]
        }
        return result
    }

    private fun classesReport(): String = reportFile("shared-classes.txt").readText()

    private fun reportFile(name: String): File {
        val dir = System.getProperty("vietlens.composeReportsDir")
            ?: fail(
                "System property `vietlens.composeReportsDir` is not set. It is configured " +
                    "in shared/build.gradle.kts for every Test task; running this class " +
                    "straight from the IDE without Gradle will not set it.",
            )
        val file = File(dir, name)
        if (!file.isFile) {
            fail(
                "Compose report `$name` is missing from $dir.\n" +
                    "It is written as a side effect of compiling :shared, so run\n" +
                    "  ./gradlew :shared:testAndroidHostTest\n" +
                    "which depends on `compileAndroidMain`, rather than the test alone.",
            )
        }
        return file
    }

    private companion object {
        /**
         * What is left after `compose-stability.conf`: the ViewModels, the platform
         * narration manager, the Android camera device, the camera controller, the shutter
         * bus, the MVI base class, the two sovereignty geometry holders and a generated
         * serializer. Every one of them is genuinely mutable or genuinely an array.
         *
         * Lowered from 20 to 19 when the voice-input branch was deleted on 02.08.2026 and
         * `AndroidSpeechRecognizerManager` went with it. The ceiling is the count as it
         * stands, not a target to grow into — leaving it at 20 would have left one free slot
         * for the next unstable class to arrive in unnoticed, which is the whole failure
         * this gate exists to catch.
         *
         * Raised back to 20 on 03.08.2026 by the MVI refactor: `SovereigntyViewModel` became
         * an `MviViewModel` and so gained a `SovereigntyState`, which holds the `RegionMap`
         * that was already unstable and is unstable for the same reason — see the note on
         * [stateClassesThatMustBeStable]. It is a state class arriving in this count, which is
         * normally exactly the regression the gate exists to catch; it is accepted here only
         * because this state is never passed to a composable and changes once in the screen's
         * life. Nothing else may use that as a precedent.
         *
         * Raised to 21 on 04.08.2026 by the tablet lens: `ZoomDriver` moved out of
         * `LensScreen.kt`, where it was `private` and invisible to this report, into
         * `feature/camera/component/ZoomDial.kt`, where both arrangements can reach it. The
         * class did not change — it holds a `glide: Job?` it cancels and replaces, so
         * unstable is the correct answer and no `compose-stability.conf` entry could honestly
         * say otherwise. What changed is that it is now counted. It is never a composable
         * parameter: the dial receives three function references off it, never the driver.
         */
        const val UNSTABLE_CLASS_CEILING = 21

        val DECLARATION = Regex("""fun ([\w.]+)\(""")
        val UNSTABLE_PARAM = Regex("""^\s+unstable (\w+):""")
    }
}
