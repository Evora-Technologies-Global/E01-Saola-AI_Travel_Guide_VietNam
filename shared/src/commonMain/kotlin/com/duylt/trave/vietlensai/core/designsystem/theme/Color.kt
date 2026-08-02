package com.duylt.trave.vietlensai.core.designsystem.theme

import androidx.compose.ui.graphics.Color

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
val LacquerRedLight = Color(0xFFDA3B32)
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
val JadeLight = Color(0xFF4E9C8A)
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
