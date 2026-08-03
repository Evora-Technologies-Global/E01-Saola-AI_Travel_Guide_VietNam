package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reading-first typography.
 *
 * The app's core content is 60-120 word passages of history read on a phone held
 * at arm's length, often one-handed while walking. Body sizes are therefore a step
 * larger than the Material default and line height is generous — and
 * [LineHeightStyle.Trim.None] keeps Vietnamese stacked diacritics (ề, ộ, ữ) from
 * being clipped, which the default trimming does at tight line heights.
 *
 * ## All fifteen scales are declared here, and that is the point
 *
 * Nine of the fifteen used to be overridden and six were left at the Material default.
 * Only the overridden nine carried [readableLineHeight] — so `bodySmall`, `labelMedium`,
 * `labelSmall`, `titleSmall`, `displayLarge` and `displayMedium` clipped the marks on
 * ề, ộ and ữ at every one of their 44 call sites, including [StampType.kicker], which
 * draws every eyebrow label in the app. A scale left at the default is not "unchanged";
 * it is a scale that silently opts out of the one rule this file exists to enforce.
 *
 * ## The two ladders
 *
 * **Weight is baked in.** No call site adds a `fontWeight`; if a variant is needed it is
 * a scale, and it lives in this file. The ladder is `display` **Bold**, `headlineLarge`
 * and `headlineMedium` **Bold**, `headlineSmall` **SemiBold** where it meets the titles,
 * `title` **SemiBold**, `label` **SemiBold** then **Medium**, `body` **Normal**. A call
 * site reaching for a heavier title is asking for a different role, not a different weight
 * — [Typography.titleMedium] is the card and row title, and the eight call sites that
 * used to bolden it were all drawing exactly that.
 *
 * **Size is bumped for reading, not for chrome.** `bodyLarge` 17, `bodyMedium` 15 and
 * `bodySmall` 13 each sit one step above Material, because they carry the passages. The
 * label ladder (14 / 12 / 11) is left where Material put it — labels are chrome, read in a
 * glance, and enlarging them costs the content its room.
 *
 * ## Role map — which scale draws what
 *
 * | Role | Scale |
 * |---|---|
 * | Page title, document screens | `headlineMedium` — set by `PageHeader`, not by the screen |
 * | Page subtitle | `bodyMedium` on `onSurfaceVariant` — also `PageHeader`'s |
 * | Overlay title, over a photo or the camera | `titleLarge` — set by `OverlayHeader` |
 * | Eyebrow, kicker, section label | [StampType.kicker] |
 * | Card / row title | `titleMedium` |
 * | Reading body | `bodyLarge` (17 / 27) — discovery narrative, chat bubbles |
 * | Secondary body | `bodyMedium` (15 / 23) — subtitles, descriptions |
 * | Metadata / caption | `bodySmall` (13 / 20) — timestamps, counts |
 * | Button | `labelLarge` |
 *
 * The cost of ignoring the map is the state this file was in before it existed: five page
 * titles drawn at four different sizes, so moving between the journal and the settings
 * read as moving between two apps.
 */
private val readableLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private val defaultTypography = Typography()

val VietLensTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    displayMedium = defaultTypography.displayMedium.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    displaySmall = defaultTypography.displaySmall.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineLarge = defaultTypography.headlineLarge.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineMedium = defaultTypography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        lineHeightStyle = readableLineHeight,
    ),
    headlineSmall = defaultTypography.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    titleLarge = defaultTypography.titleLarge.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    titleMedium = defaultTypography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    titleSmall = defaultTypography.titleSmall.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.15.sp,
        lineHeightStyle = readableLineHeight,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.2.sp,
        lineHeightStyle = readableLineHeight,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
        lineHeightStyle = readableLineHeight,
    ),
    labelLarge = defaultTypography.labelLarge.copy(
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = readableLineHeight,
    ),
    labelMedium = defaultTypography.labelMedium.copy(
        fontWeight = FontWeight.Medium,
        lineHeightStyle = readableLineHeight,
    ),
    labelSmall = defaultTypography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        lineHeightStyle = readableLineHeight,
    ),
)

/**
 * The stamped-on family: monospaced chrome, for the places the app imitates a printed
 * label rather than setting text.
 *
 * Three of the four are monospaced on purpose — beside the proportional body type they read
 * as captions stamped onto the page, the way a museum label sits beside its object. [seal] is
 * the exception and stays proportional: at 7 sp inside a drawn ring, monospace's even advance
 * widths cost more room than the stamp has. They are not a
 * sixteenth, seventeenth and eighteenth typography scale invented at three call sites,
 * which is what they were: the kicker's mono/bold/tracking lived in `Components.kt`, the
 * seal re-`copy()`-ed `labelSmall` inside a component file, and the passport stamp and
 * the discovery ordinal each rebuilt the same monospace numeral by hand. A style defined
 * where it is drawn drifts from its siblings within a fortnight and nobody notices,
 * because no two of them are ever on screen at once.
 *
 * Each derives from a real scale above, so all four inherit [readableLineHeight] and the
 * diacritic fix comes with them.
 */
object StampType {

    /**
     * Small caps with wide tracking — the label that names a block without shouting.
     *
     * The workhorse: roughly thirty call sites, every eyebrow and every section heading.
     * Draw it through the `Kicker` composable, which also does the uppercasing.
     */
    val kicker: TextStyle = VietLensTypography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )

    /**
     * The mono numeral: "01" down a numbered list, and the province name on a passport
     * stamp. One step up from [kicker] because it is the thing being read, not the label
     * naming it.
     */
    val ordinal: TextStyle = VietLensTypography.labelMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        lineHeight = 15.sp,
    )

    /** The date under a stamp, and anything else set in mono at caption size. */
    val caption: TextStyle = VietLensTypography.labelSmall.copy(
        fontFamily = FontFamily.Monospace,
    )

    /**
     * Seven point, inside the circular sovereignty seal.
     *
     * The one place in the app that goes below the label ladder, because the text has to
     * fit inside a drawn ring rather than a line of type. Vietnamese stacks marks two deep
     * on Ủ and Ề and the default trimming clips them off at a line height this tight —
     * which is [readableLineHeight]'s whole job, inherited here rather than re-declared.
     */
    val seal: TextStyle = VietLensTypography.labelSmall.copy(
        fontSize = 7.sp,
        lineHeight = 9.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.4.sp,
    )
}

/**
 * The five radii [VietLensShapes] is built from, as numbers rather than `Shape`s.
 *
 * For everything that has to trace a corner **by hand** and therefore cannot be handed a
 * `Shape`: a `drawBehind` that strokes a dashed outline, a `drawRoundRect`, a hairline
 * along a sheet's lip. Those have to agree with the `clip` on the very same node to the
 * pixel — an outline drawn at 20 while its box is clipped at 24 has its four corner arcs
 * sliced off, and it reads as a rendering fault rather than as a style choice.
 *
 * Reading the shape and reading the number therefore come from one source. Use
 * `MaterialTheme.shapes` wherever a `Shape` will do; reach for these only when the value
 * has to enter a draw scope.
 */
object Corner {
    val extraSmall = 6.dp
    val small = 10.dp
    val medium = 16.dp
    val large = 24.dp
    val extraLarge = 32.dp
}

/**
 * Five radii for the whole app, and every corner drawn in it is one of them.
 *
 * These slots were declared and passed to `MaterialTheme` on the first day and then
 * referenced zero times: the app drew fifty-seven `RoundedCornerShape(N.dp)` literals at
 * twelve distinct radii instead, seven of which are not in this list. The most common
 * corner in the app was 20 dp — a value the theme has never had.
 *
 * The twelve collapse onto the five as follows, and the mapping is the standard:
 *
 * | Slot | Radius | Absorbs | For |
 * |---|---|---|---|
 * | `extraSmall` | 6 | 3, 4, 6 | tags, tiny chips |
 * | `small` | 10 | 10, 12 | inline controls |
 * | `medium` | 16 | 14, 16, 18 | cards, sheets-in-page |
 * | `large` | 24 | 20, 24 | big cards, the camera frame |
 * | `extraLarge` | 32 | 28, 32 | the passport sheet, the chat composer |
 *
 * The value of doing this at all is in the row above: if 24 turns out to read too soft
 * where 20 used to be, it is one number in this file rather than seventeen call sites.
 */
val VietLensShapes = Shapes(
    extraSmall = RoundedCornerShape(Corner.extraSmall),
    small = RoundedCornerShape(Corner.small),
    medium = RoundedCornerShape(Corner.medium),
    large = RoundedCornerShape(Corner.large),
    extraLarge = RoundedCornerShape(Corner.extraLarge),
)


/**
 * The fully-round end cap, for chips and pills.
 *
 * Not a slot in [VietLensShapes] because it is not a radius: it is a percentage, and it
 * has to stay one. A pill given a fixed corner stops being a pill the moment its content
 * makes it shorter than twice that corner — which is exactly what happens to a filter
 * chip when the locale changes and the label wraps.
 */
val Pill = RoundedCornerShape(percent = 50)
