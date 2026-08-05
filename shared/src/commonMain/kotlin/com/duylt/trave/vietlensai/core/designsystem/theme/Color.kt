package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

/**
 * A palette drawn from Vietnamese material culture rather than from a generic
 * travel-app blue: lacquer red, temple gold, jade, and the warm sand of old
 * Hội An walls.
 *
 * The app is used outdoors in bright sun and inside dim temples, so both schemes
 * keep body text at or above a 7:1 contrast ratio against their surfaces.
 */

// Lacquer red — the primary accent, from sơn mài lacquerware and temple gates.
val LacquerRed = Color(0xFFB3261E)
val LacquerRedDark = Color(0xFF8C1D18)
val LacquerContainer = Color(0xFFFFDAD6)
val LacquerContainerDark = Color(0xFF73342D)

// Temple gold — secondary accent, used for favourites and highlights.
val TempleGold = Color(0xFFB58A28)
val TempleGoldLight = Color(0xFFE0B04A)
val TempleGoldContainer = Color(0xFFFFE9B8)
val TempleGoldContainerDark = Color(0xFF5C4405)

// Jade — tertiary, for translation and "verified" affordances.
val Jade = Color(0xFF2E6B5E)
val JadeContainer = Color(0xFFB9F0E0)
val JadeContainerDark = Color(0xFF124F44)

// Neutrals warmed slightly towards sand so photos sit on the page rather than float.
val SandLightest = Color(0xFFFFFBF8)
val SandLight = Color(0xFFF6EFE8)
val SandMid = Color(0xFFE6DDD4)
val InkDarkest = Color(0xFF15120F)
val InkDark = Color(0xFF201B17)
val InkMid = Color(0xFF2C2622)
val InkSoft = Color(0xFF52443C)

/**
 * The lens design's own four: paper, marigold, vermilion, ink.
 *
 * Fixed rather than taken from the colour scheme because the camera screen is
 * always dark — it is a viewfinder, not a surface — so a light/dark scheme would
 * change its character rather than merely its brightness. Marigold on ink is the
 * pairing every control on that screen uses, and it clears 7:1 either way round.
 */
val PaperCream = Color(0xFFFBF6EC)
val Marigold = Color(0xFFE3A93C)
val Vermilion = Color(0xFFB4181F)
val InkBrown = Color(0xFF2A1A12)

/**
 * The seven tints the conversation with the guide is drawn in.
 *
 * All derived from the four above, and fixed in both schemes for the same reason the lens is:
 * the guide is a page in the same notebook as the passport and the journal cards, and a dark
 * variant would make it a different object rather than a dimmer one. `LLM.md` §11 row #16 is
 * the standing note that this is a colour-system decision nobody has taken yet — and it asked
 * for exactly this, the palette written up here beside the lens and the flag rather than left
 * as seven `private val`s at the top of one screen.
 *
 * They moved here when the tablet gained a guide **column**: two branches drawing the same
 * conversation cannot each own a copy of its colours, or the pair drift and the phone and the
 * iPad end up two shades of cream apart. Whoever eventually converts this screen to the colour
 * scheme changes this object and nothing else.
 */
object GuidePalette {
    /** The paper the whole conversation sits on. */
    val page = PaperCream

    /** Behind the heading — a breath of marigold, so the title band is not the same cream. */
    val header = Marigold.copy(alpha = 0.10f).compositeOver(PaperCream)

    /** Behind the input row, warmed the other way so the two ends of the page differ. */
    val composer = Vermilion.copy(alpha = 0.07f).compositeOver(PaperCream)

    /** The guide's own bubbles: a shade lighter than the page, so they lift off it. */
    val card = Color(0xFFFFFCF6)
    val cardBorder = Marigold.copy(alpha = 0.35f)

    /** Ink stepped back for metadata — the subtitle, the kicker, the placeholder. */
    val inkMuted = InkBrown.copy(alpha = 0.60f)

    /** The text field's resting edge. */
    val hairline = InkBrown.copy(alpha = 0.10f)

    /** Behind the back chip, which on this page is ink on paper rather than a scheme surface. */
    val backChip = InkBrown.copy(alpha = 0.07f)
}

/**
 * The national flag, in its own two colours rather than in the app's.
 *
 * Deliberately outside the palette above and never theme-derived. Everything else on
 * these screens is lacquer red and temple gold because the design chose them; this is
 * cờ đỏ sao vàng, and a flag drawn in an approximate red is simply the wrong flag. It
 * does not lighten in the dark scheme and it does not fade on a province that has not
 * been visited — the flag is not a reward the traveller unlocks.
 */
val FlagRed = Color(0xFFDA251D)
val FlagYellow = Color(0xFFFFFF00)

val ErrorRed = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val ErrorRedDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)

/** Per-category accents used by chips and the journal timeline. */
object CategoryColors {
    val landmark = Color(0xFFB3261E)
    val food = Color(0xFFD97706)
    val artifact = Color(0xFF7C5CBF)
    val architecture = Color(0xFF2563A8)
    val nature = Color(0xFF2E7D4F)
    val culture = Color(0xFFB58A28)
    val other = Color(0xFF6B5F57)
}
