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
     *  - `navController` — same shape, owned by the nav host.
     *  - `controller` — [com.duylt.trave.vietlensai.feature.camera.CameraController], which
     *    wraps the platform camera session and is `remember`ed for the screen's lifetime.
     *  - `model: Any?` on `AppAsyncImage` — Coil's model parameter is `Any?` by design.
     *  - `map` — the sovereignty outlines, `List<DoubleArray>`. Arrays are mutable, so
     *    unstable is the *correct* answer; the value is parsed from an asset once and
     *    remembered, never edited.
     *
     * Adding to this list is a deliberate act. A state class turning up here means a screen
     * has stopped skipping, and the fix is `compose-stability.conf`, not this allowlist.
     */
    private val allowedUnstableParams: Map<String, Set<String>> = mapOf(
        "AppAsyncImage" to setOf("model"),
        "CameraPreview" to setOf("controller"),
        "LensRoute" to setOf("viewModel"),
        "LensScreen" to setOf("controller"),
        "Viewfinder" to setOf("controller"),
        "rememberZoomDriver" to setOf("controller"),
        "ChatRoute" to setOf("viewModel"),
        "CollectionRoute" to setOf("viewModel"),
        "DiscoveryRoute" to setOf("viewModel"),
        "ExploreRoute" to setOf("viewModel"),
        "JournalRoute" to setOf("viewModel"),
        "PassportRoute" to setOf("viewModel"),
        "SettingsRoute" to setOf("viewModel"),
        "SovereigntyRoute" to setOf("viewModel"),
        "TranslationRoute" to setOf("viewModel"),
        "SovereigntyMapCanvas" to setOf("map"),
        "MapCard" to setOf("map"),
        "VietLensApp" to setOf("navController"),
        "VietLensNavHost" to setOf("navController"),
    )

    /**
     * State classes that must stay stable, named individually.
     *
     * These are the ones a screen passes wholesale to its content composable, so each is
     * the single point at which that screen either skips or does not. Checking them by name
     * rather than relying on the aggregate count means the failure message says *which*
     * screen regressed.
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
         * What is left after `compose-stability.conf`: ten ViewModels, the two platform
         * voice managers, the Android camera device, the camera controller, the shutter
         * bus, the MVI base class, the two sovereignty geometry holders and a generated
         * serializer. Every one of them is genuinely mutable or genuinely an array.
         */
        const val UNSTABLE_CLASS_CEILING = 20

        val DECLARATION = Regex("""fun ([\w.]+)\(""")
        val UNSTABLE_PARAM = Regex("""^\s+unstable (\w+):""")
    }
}
