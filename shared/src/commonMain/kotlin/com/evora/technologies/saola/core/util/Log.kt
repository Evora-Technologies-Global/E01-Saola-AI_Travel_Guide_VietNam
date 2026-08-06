package com.evora.technologies.saola.core.util

import co.touchlab.kermit.Logger

/**
 * The presentation layer's logger.
 *
 * Kermit rather than Timber: Timber is built on `android.util.Log` and cannot compile for
 * Kotlin/Native at all. Kermit keeps the same one-line ergonomics and routes to the
 * platform's own sink — logcat on Android, `os_log` on iOS.
 *
 * The message is a lambda, so building a string costs nothing when the level is off.
 */
internal val log: Logger = Logger.withTag("Saola")
