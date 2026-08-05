package com.evora.technologies.saola.data.platform

/**
 * How long either platform waits for a fresh fix before falling back to the last known one.
 *
 * Shared so the two implementations cannot drift: location only sharpens the answer, so
 * waiting on a cold GPS fix inside a museum would trade a real feature for a marginal one.
 * A stale last-known fix is more than accurate enough to tell Hanoi from Hội An.
 */
internal const val LOCATION_FIX_TIMEOUT_MILLIS = 4_000L
