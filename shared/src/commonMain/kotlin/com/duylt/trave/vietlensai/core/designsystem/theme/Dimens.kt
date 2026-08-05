package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * The seven gaps the app is allowed to leave between two things.
 *
 * Before this existed there was one token, [ScreenGutter], and then 195 literals: 92
 * `Spacer(Modifier.height(N.dp))` at sixteen distinct values, 64 `padding(vertical = N.dp)`
 * at twelve, 39 `padding(horizontal = N.dp)` at eleven. Sixteen values is not a spacing
 * system with exceptions in it; it is the absence of one. Nothing on screen distinguishes
 * a 14 dp gap from a 16 dp gap, but the eye reads the page they are on as slightly
 * unresolved, and no reviewer can say which of the two is the mistake.
 *
 * Four dp steps because that is the grid the platform's own touch targets and icon sizes
 * are cut on, so a column built from these lands on the same rhythm as the components
 * inside it.
 *
 * Padding *inside* a card or a chip is that component's own business and does not have to
 * come from here — but it usually should, and a component reaching for a value between two
 * steps is normally a component asking for the wrong step.
 */
object Spacing {
    /** Between two lines of a single block — a title and the label right under it. */
    val xxs = 2.dp

    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp

    /** The default gap, and the same number as [ScreenGutter]. */
    val lg = 16.dp

    val xl = 24.dp
    val xxl = 32.dp
}

/**
 * The four gaps that make a page a page, named by what they separate.
 *
 * These are the ones that have to agree *across* screens rather than within one. Five
 * screens each decided their own header box — 0/16, 0/4, 12/4, 12/4 and 10/12 — and the
 * traveller moves between the lens, the journal and a discovery in seconds, so the title
 * jumping a few dp from tab to tab reads as the app losing its footing.
 *
 * `PageHeader` applies [headerTop] and [headerToContent] itself; a screen never types
 * them. [listBottom] is the value that already existed in fact, written out as
 * `PaddingValues(bottom = 32.dp)` in three separate `LazyColumn`s, and had no name.
 */
object PageSpacing {
    /** Inset edge to the top of the header. */
    val headerTop = Spacing.md

    /** Header to the first row of content below it. */
    val headerToContent = Spacing.lg

    /** Between blocks within a page. */
    val sectionGap = Spacing.xl

    /** Under the last item of a scrolling list, so it clears the bottom bar. */
    val listBottom = Spacing.xxl

    /**
     * How far a snackbar is lifted off the bottom edge on a tab screen.
     *
     * Not a gap between two things and so not a [Spacing] step: it is the height of the
     * navigation bar plus a gutter, measured against the bar rather than chosen. The
     * journal and explore both raise their snackbar by exactly this, and they used to
     * do it with the same literal typed twice — which is one screen away from the two
     * drifting and a failure notice sitting on top of the tab it is about.
     */
    val snackbarLift = 96.dp
}

/**
 * How wide each pane is in the large-window arrangement.
 *
 * These are **measured positions, not gaps**, so they are deliberately not on the [Spacing]
 * scale — the same argument `SHUTTER_INSET` and `SheetPeekHeight` are held to. Each one was
 * read off the tablet wireframe (`Việt Travel Lens - Tablet.dc.html`, 1194 × 834 with a
 * 1218 px inner frame) rather than chosen, and the frame is what they are measured against:
 * take [rail] off 1218 and 1114 is left for content, which is the number every other value
 * here is a fraction of.
 *
 * They live in one object rather than beside the five screens that draw them because the
 * proportions only make sense read together. The guide column is 352 of 1114 — 31.6% — and
 * that split is a judgement about *both* sides: enough for a question and its answer to be
 * readable, and not so much that the story beside it drops under a comfortable measure. Move
 * one of these and the one it sits next to has to be looked at in the same minute.
 *
 * `tablet/navigation/TwoPaneScaffold.kt` is the only file that reads [lensPanel], [guide] and
 * [journalList]; a screen names the width it wants and hands it over.
 */
object PaneWidth {
    /** The navigation rail down the left edge, and the only one of these on every screen. */
    val rail = 104.dp

    /** The controls beside the viewfinder — modes, recent captures, what the lens found. */
    val lensPanel = 310.dp

    /** The AI guide, alongside a discovery rather than pushed on top of it. */
    val guide = 352.dp

    /** The day column of the journal — the master half, which sits at the **start**. */
    val journalList = 392.dp

    /**
     * A modal that floats over the arrangement instead of covering it.
     *
     * The only one of these five that is not a pane, and so the only one `TwoPaneScaffold`
     * never sees. Three things read it, and all three are the same decision: the theme picker
     * and the clear-history confirmation cap themselves here rather than taking Material's
     * 560 dp, and the sovereignty statement sets its document column to it. `widthIn` in every
     * case, so a phone — narrower than this to begin with — is left exactly as it was.
     */
    val sheet = 440.dp
}

/**
 * The gutter every screen keeps between the display edge and anything drawn on it.
 *
 * One value for the whole app rather than a per-screen choice: the traveller moves
 * between the lens, the journal and a discovery in seconds, and edges that shift by
 * a few dp from screen to screen read as a mistake even when nobody can say which
 * screen is the wrong one.
 *
 * It applies to the outermost run of a screen's content — page columns, list
 * content padding, floating controls, full-bleed banners. Padding *inside* a card
 * or a chip is that component's own business and is not this number.
 *
 * **A screen may not adjust it.** Two settings rows used to be laid out on
 * `ScreenGutter + 4.dp`, which is the token being locally corrected — and a token that
 * gets locally corrected is a token that has stopped meaning anything. If a component
 * needs to sit further in than the gutter, that is the component's own inner padding.
 */
val ScreenGutter = Spacing.lg
