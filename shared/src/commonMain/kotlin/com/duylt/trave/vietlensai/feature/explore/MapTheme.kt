package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.ui.graphics.Color

/**
 * Whether the app is currently in its dark scheme.
 *
 * Read off the surface colour rather than from `isSystemInDarkTheme()`, because the
 * traveller can force light or dark in Settings regardless of the system — and the map
 * has to match the app around it, not the OS.
 *
 * Shared rather than private to one arrangement: both branches hand the answer to
 * [PlaceMap], and the phone deciding "dark" where the tablet decided "light" would be two
 * map styles for one setting. Not `Color.luminance()`, which is the gamma-corrected Rec.709
 * figure — this is the flat Rec.601 weighting the map style was chosen against, and swapping
 * it would move the threshold under a scheme nobody re-checked.
 */
internal fun Color.isDark(): Boolean =
    (LUMINANCE_RED * red + LUMINANCE_GREEN * green + LUMINANCE_BLUE * blue) < 0.5f

private const val LUMINANCE_RED = 0.299f
private const val LUMINANCE_GREEN = 0.587f
private const val LUMINANCE_BLUE = 0.114f
