package com.duylt.trave.vietlensai.platform

/**
 * True on iOS.
 *
 * Only for the handful of places where the *content* differs rather than the implementation —
 * the maps URL scheme, which Apple claims as `maps://` and Android as `geo:`. Anything larger
 * than a string belongs in an `expect` declaration instead, where the compiler enforces that
 * both platforms are handled.
 */
internal expect val IS_APPLE_PLATFORM: Boolean
