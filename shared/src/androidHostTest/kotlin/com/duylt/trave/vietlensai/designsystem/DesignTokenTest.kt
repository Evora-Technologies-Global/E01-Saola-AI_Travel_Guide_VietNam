package com.duylt.trave.vietlensai.designsystem

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The design-token gate.
 *
 * A hardcoded radius and `MaterialTheme.shapes.large` compile to the same bytecode, render
 * the same pixels, and pass every other test in this project. The difference between them
 * only exists in the source file, which is why this test reads the sources as text.
 *
 * It is here because the standardisation it protects was expensive and is trivially
 * reversible. Before it, `:shared` held 57 `RoundedCornerShape(N.dp)` literals at twelve
 * distinct radii, 195 spacing literals at nineteen distinct values, and 44 call-site
 * `fontWeight`s. None of that was written by someone being careless: each one was a
 * reasonable local decision, taken in a file where the alternative was not visible. That
 * is exactly the class of drift a reviewer cannot catch by reading a diff — the diff looks
 * fine — and exactly the class a text check catches in a second.
 *
 * **Every rule below states the cost as well as the location.** A gate whose message reads
 * "violation at line 12" gets `@Ignore`d the first week it is inconvenient.
 *
 * @see com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
 * @see com.duylt.trave.vietlensai.core.designsystem.theme.VietLensShapes
 */
class DesignTokenTest {

    // -------------------------------------------------------------------------------
    // Rules
    // -------------------------------------------------------------------------------

    @Test
    fun `no corner radius is written at a call site`() {
        assertNoMatches(
            files = featureFiles(),
            pattern = CORNER,
            rule = "A corner radius is written out in a feature file.",
            cost = """
                The app draws five radii and nothing else: MaterialTheme.shapes.extraSmall
                (6), small (10), medium (16), large (24) and extraLarge (32), plus `Pill`
                for a fully-round end cap. A literal here is a sixth, and it stays a sixth
                — there were twelve of them before this gate, seven of which the theme had
                never heard of, and the single most common corner in the app was a value
                `VietLensShapes` did not contain. Reversing one of those choices meant
                editing seventeen call sites instead of one line of `Type.kt`.

                Use MaterialTheme.shapes.X, or `Pill`, or `CircleShape`. If a shape genuinely
                has to be a number — a curve traced by hand in a `drawBehind` — read it from
                a named constant from `Corner` so the shape and the drawing cannot drift.
            """.trimIndent(),
        )
    }

    @Test
    fun `no gap is written at a call site`() {
        assertNoOffenders(
            offenders = featureFiles().flatMap { gapLiterals(it) },
            rule = "A gap is written out in a feature file.",
            cost = """
                Spacing comes from `Spacing` (2/4/8/12/16/24/32) or from `PageSpacing`,
                which names the four gaps that have to agree across screens. There used to
                be 92 `Spacer`s at sixteen distinct heights, 64 vertical paddings at twelve,
                and 39 horizontal ones at eleven. Nothing on any single screen looked wrong;
                the app looked wrong, and no reviewer could say which of a 14 dp gap and a
                16 dp gap was the mistake.

                A value that happens to equal a step is still a literal — `16.dp` reads the
                same as `Spacing.lg` today and stops matching it the moment the scale moves,
                and nobody will find it when that happens.

                If the number is a *position* rather than a gap — the room a floating
                composer takes, where a shutter sits above the navigation bar — it does not
                belong on a 4 dp scale at all. Give it a named `private val` with a sentence
                saying what it was measured against.
            """.trimIndent(),
        )
    }

    @Test
    fun `no weight is applied on top of a typography scale`() {
        assertNoMatches(
            files = featureFiles(exceptNamed = MEASURED_TYPE),
            pattern = WEIGHT,
            rule = "A font weight is set in a feature file.",
            cost = """
                Every one of the fifteen scales in `Type.kt` carries its own weight, on a
                ladder: display Bold, headlineLarge/Medium Bold, headlineSmall SemiBold,
                title SemiBold, label SemiBold then Medium, body Normal. A call site adding
                a weight is asking for a role the
                map does not have — and it gets it privately, so the next screen that wants
                the same thing writes its own.

                Pick the scale whose role matches. If none does, the variant is a scale and
                it belongs in `Type.kt`, where the whole app can find it — which is what
                `StampType` is: the monospace chrome that used to be re-derived at four
                separate call sites.
            """.trimIndent(),
        )
    }

    @Test
    fun `no type size is written at a call site`() {
        assertNoMatches(
            files = featureFiles(exceptNamed = MEASURED_TYPE),
            pattern = TYPE_SIZE,
            rule = "A type size is written out in a feature file.",
            cost = """
                A size written here is a sixteenth typography scale that only one screen
                knows about. It also silently drops `LineHeightStyle.Trim.None`, which every
                scale in `Type.kt` carries and which is the reason Vietnamese stacked
                diacritics — ề, ộ, ữ — are not clipped at this app's line heights.

                The allowlisted files are the ones that *measure* a size rather than choose
                one: ${MEASURED_TYPE.joinToString()}. A map label is sized from the
                projection, and a translation block is sized from the line of Vietnamese it
                is covering. Adding to that list is a deliberate act, not a way past a
                failure.
            """.trimIndent(),
        )
    }

    @Test
    fun `the top inset comes from screenInsetsPadding`() {
        assertNoMatches(
            files = commonMainFiles(),
            pattern = RAW_INSET,
            rule = "`statusBarsPadding()` is used instead of `screenInsetsPadding()`.",
            cost = """
                This app hides the system bars, so the status-bar inset is **zero** — and
                the notch is a hole in the glass that is still there. `statusBarsPadding()`
                therefore compiles, runs, and pads by nothing, and the control sits under
                the cutout on every phone that has one. That is not hypothetical: the
                discovery page did exactly this, in five places, until `OverlayHeader` took
                the inset over.

                `screenInsetsPadding()` unions the display cutout in and is the only correct
                answer here. On an immersive screen you usually want neither — use
                `OverlayHeader`, which applies it for you.
            """.trimIndent(),
        )
    }

    @Test
    fun `every screen renders one of the two headers`() {
        val found = featureFiles().filter { it.name in HEADER_OWNERS }
        val missing = HEADER_OWNERS - found.map { it.name }.toSet()
        assertTrue(
            missing.isEmpty(),
            "Screen file(s) named in HEADER_OWNERS no longer exist: $missing. A renamed screen " +
                "silently drops out of this check, which is how a hand-rolled header gets back in. " +
                "Rename the entry, or delete it and say why in LLM.md §13.",
        )

        val offenders = found
            .filterNot { file ->
                // The call, not the mention: a screen declaring its own `private fun PageHeader`
                // would satisfy a bare substring search while being the exact thing this forbids.
                val body = file.readLines().filterNot { it.isSuppressed() }.joinToString("\n")
                HEADER_CALL.containsMatchIn(body)
            }
            .map { it.name }

        assertTrue(
            offenders.isEmpty(),
            """
            ${offenders.size} screen(s) draw a header of their own.

            There are two, and only two: `PageHeader` for a document screen and
            `OverlayHeader` for one drawing over a photograph, a camera feed or a map.
            Five screens used to build their own, and the boxes agreed on nothing — top
            and bottom padding ran 0/16, 0/4, 12/4, 12/4 and 10/12, and the title was set
            at four different sizes across five pages. A traveller crosses three of them
            in about four seconds.

            Offenders: $offenders
            """.trimIndent(),
        )
    }

    // -------------------------------------------------------------------------------
    // Machinery
    // -------------------------------------------------------------------------------

    /**
     * Every `.dp` literal that is an **argument to** a spacing call.
     *
     * A paren-matching scan rather than a regex, because the two things this has to get right
     * pull in opposite directions and no single expression does both.
     *
     * It must see through a nested paren: `Spacer(Modifier.height(if (index == 0) 16.dp else
     * 14.dp))` hides its literals behind the condition's own bracket, and the first version of
     * this rule — which scanned with `[^)\n]*` — stopped dead at it. Three such Spacers, one
     * `PaddingValues(vertical = 14.dp)` and a `pageSpacing = 12.dp` sat in `feature/` while the
     * gate reported clean. `PaddingValues(` was missed for a second reason: the pattern looked
     * for a lowercase `padding(`, and the capital P is not that.
     *
     * And it must NOT fire on a size that merely shares a line with a gap:
     * `Box(Modifier.padding(top = Spacing.xxs).size(14.dp))` is correct code, and a rule that has
     * to be suppressed on correct code is a rule that gets suppressed everywhere. Matching the
     * call's own balanced argument list is what separates the two.
     *
     * The same chain on a `Spacer` **does** fire, and that is right rather than a false positive:
     * a `Spacer` has no content, so whatever sizes it *is* the gap. Read an offender pointing at
     * a `Spacer(Modifier.…size(N.dp))` as the rule working, not as something to suppress.
     */
    private fun gapLiterals(file: File): List<String> {
        val lines = file.readLines()
        // Comment lines are blanked rather than dropped, so line numbers stay true — and the
        // offset table is built from the *blanked* text, not the original. Building it from the
        // original made every reported line number drift earlier by the length of the comments
        // above it, which points a developer at innocent code and is worse than not reporting.
        val scrubbed = lines.map { if (it.isSuppressed()) "" else it }
        val text = scrubbed.joinToString("\n")
        val lineStart = scrubbed.runningFold(0) { acc, line -> acc + line.length + 1 }

        fun report(offset: Int): String {
            val index = lineStart.indexOfLast { it <= offset }.coerceIn(lines.indices)
            return "${file.name}:${index + 1}  ${lines[index].trim()}"
        }

        val found = mutableListOf<String>()

        GAP_CALL.findAll(text).forEach { call ->
            val open = call.range.last + 1
            if (open >= text.length || text[open] != '(') return@forEach
            var depth = 0
            var cursor = open
            while (cursor < text.length) {
                when (text[cursor]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                if (depth == 0) break
                cursor++
            }
            val args = text.substring(open + 1, cursor.coerceAtMost(text.length))
            DP_LITERAL.find(args)?.let { found += report(open + 1 + it.range.first) }
        }

        GAP_ASSIGN.findAll(text).forEach { found += report(it.range.first) }

        return found.distinct()
    }

    private fun assertNoOffenders(offenders: List<String>, rule: String, cost: String) {
        assertTrue(
            offenders.isEmpty(),
            buildString {
                appendLine("$rule (${offenders.size} occurrence(s))")
                appendLine()
                appendLine(cost)
                appendLine()
                offenders.forEach { appendLine("  - $it") }
            },
        )
    }

    private fun assertNoMatches(files: List<File>, pattern: Regex, rule: String, cost: String) {
        val offenders = files.flatMap { file ->
            file.readLines()
                .withIndex()
                .filterNot { (_, line) -> line.isSuppressed() }
                .filter { (_, line) -> pattern.containsMatchIn(line) }
                .map { (index, line) -> "${file.name}:${index + 1}  ${line.trim()}" }
        }

        assertTrue(
            offenders.isEmpty(),
            buildString {
                appendLine("$rule (${offenders.size} occurrence(s))")
                appendLine()
                appendLine(cost)
                appendLine()
                offenders.forEach { appendLine("  - $it") }
            },
        )
    }

    /**
     * Comments are skipped, so a rule can be *described* in the file it governs.
     *
     * Without this, writing "…rather than `RoundedCornerShape(20.dp)`" in a KDoc explaining
     * why a screen now uses the token fails the very gate the comment is about — and the
     * fix people reach for is deleting the explanation.
     */
    private fun String.isSuppressed(): Boolean {
        val trimmed = trimStart()
        return trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
    }

    private fun featureFiles(exceptNamed: List<String> = emptyList()): List<File> =
        sourceRoot().resolve("com/duylt/trave/vietlensai/feature")
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { it.name.removeSuffix(".kt") in exceptNamed }
            .toList()

    private fun commonMainFiles(): List<File> =
        sourceRoot().walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()

    private fun sourceRoot(): File {
        val path = System.getProperty("vietlens.commonMainDir")
            ?: fail(
                "System property `vietlens.commonMainDir` is not set. It is configured in " +
                    "shared/build.gradle.kts for every Test task; running this class straight " +
                    "from the IDE without Gradle will not set it.",
            )
        val dir = File(path)
        if (!dir.isDirectory) fail("commonMain sources are not at $path.")
        return dir
    }

    private companion object {
        /**
         * Files that compute a type size instead of choosing one.
         *
         * `VietnamMapCanvas` and `SovereigntyMap` label a projection drawn on a `Canvas`,
         * where the text is part of the drawing and `drawText` takes a `TextStyle` rather
         * than reading the theme. `TranslationScreen` sizes each translated block from the
         * height of the Vietnamese line it is covering — English runs longer than the
         * Vietnamese it replaces, and a fixed scale would either overflow the box or waste
         * it. None of the three is a style decision that could live in `Type.kt`.
         */
        val MEASURED_TYPE = listOf("VietnamMapCanvas", "SovereigntyMap", "TranslationScreen")

        /** The spacing calls whose *arguments* may not contain a literal. */
        val GAP_CALL = Regex("""\b(?:padding|PaddingValues|spacedBy|Spacer)$""".dropLast(1))

        /** Spacing passed as a named value rather than as a call argument. */
        val GAP_ASSIGN = Regex("""\b(?:pageSpacing|itemSpacing)\s*=\s*\d+(?:\.\d+)?\.dp""")

        /**
         * A `.dp` literal, `0.dp` excepted — zero is the absence of a gap, not a choice of one.
         */
        val DP_LITERAL = Regex("""(?<![\w.])(?!0\.dp\b)\d+(?:\.\d+)?\.dp\b""")

        /**
         * A corner written as a number.
         *
         * Matches the named-argument spellings (`topStart = 20.dp`) as well as the positional
         * one, and covers `CutCornerShape` — the same decision taken with a different corner
         * treatment is still a sixth radius.
         */
        val CORNER = Regex("""(?:Rounded|Cut)CornerShape\([^\n]*?(?:\d+(?:\.\d+)?\.dp|percent\s*=\s*\d)""")

        val WEIGHT = Regex("""fontWeight\s*=""")

        val TYPE_SIZE = Regex("""\d+(?:\.\d+)?\.sp\b""")

        /**
         * The raw inset, in either spelling.
         *
         * `windowInsetsPadding(WindowInsets.statusBars)` is the same defect written longhand, and
         * `Insets.kt` uses `windowInsetsPadding` legitimately — so the union with `displayCutout`
         * is what makes it correct, and this pattern requires `statusBars` NOT to be part of one.
         */
        val RAW_INSET = Regex("""\bstatusBarsPadding\(\)|windowInsetsPadding\(\s*WindowInsets\.statusBars\s*\)""")

        /** A *call* to one of the two headers, not merely the name appearing in the file. */
        val HEADER_CALL = Regex("""(?<!fun )\b(?:Page|Overlay)Header\(""")

        /**
         * The screens that must render one of the two headers.
         *
         * Eight of the ten. `LensScreen` is out because the camera tool row is not a
         * header — it is a row of switches that happens to sit at the top, and giving it a
         * title would put a caption on a viewfinder. `SovereigntyScreen` is out because it
         * is a scrolling document whose outermost column already takes the inset; it uses
         * the shared `OverlayIconButton` for its close affordance and needs nothing else.
         * Both exclusions are decisions, not omissions.
         */
        val HEADER_OWNERS = listOf(
            "JournalScreen.kt",
            "SettingsScreen.kt",
            "CollectionScreen.kt",
            "PassportScreen.kt",
            "ChatScreen.kt",
            "DiscoveryScreen.kt",
            "TranslationScreen.kt",
            "ExploreScreen.kt",
        )
    }
}
