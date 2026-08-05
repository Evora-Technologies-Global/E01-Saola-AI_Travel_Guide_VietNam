package com.evora.technologies.saola.core.util

/**
 * How long a detect may keep the traveller waiting before it is called off.
 *
 * The transport already has a budget of its own — ninety seconds, set so a Pro
 * model under load can finish rather than be cut off mid-answer. This is a
 * different promise, made by the screen rather than by the socket: while a detect
 * runs, the viewfinder is behind a scrim and the shutter is dead, and the person
 * holding the phone is standing in front of the thing they just photographed. A
 * minute and a half of that is indistinguishable from a hung app, and the useful
 * answer at that point is "try again", not a slower success.
 *
 * Thirty seconds is where the two meet. It started at fifteen, which was set by
 * eye rather than against the payload and fired on almost every capture: the
 * recognition schema asks for three to five sections of sixty to a hundred and
 * twenty words, and `generateContent` hands back nothing until the last token of
 * that is written. Fifteen seconds sat under the median, so the ceiling was
 * cancelling healthy requests and reporting them as failures.
 *
 * If this number ever needs raising again, the honest fix is upstream — ask the
 * model for less, or stream the answer — rather than another few seconds here.
 *
 * It covers the whole round-trip, not just the HTTP call — a location fix and the
 * database write are part of the wait as far as the scrim is concerned.
 */
const val DETECT_TIMEOUT_MILLIS = 30_000L
