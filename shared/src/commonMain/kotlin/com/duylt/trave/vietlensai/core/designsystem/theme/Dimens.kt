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
