package com.evora.technologies.saola.domain.util

/**
 * How the app introduces itself to the public services it depends on.
 *
 * Overpass and Wikimedia both ask callers to identify themselves, and both throttle or
 * refuse a generic library string first. Wikimedia is the blunt one: Commons answers
 * `403` to `okhttp/4.12.0`, which is what the image loader sends by default, and the
 * symptom is every photograph on a screen rendering as the broken-image mark with
 * nothing in the logs to say why.
 *
 * **The contact details are the point, not decoration.** They are what lets an operator
 * ask this app to stop rather than block the address range it arrived from. Keep the
 * address reachable — a dead one is worse than none, because it reads as an answer.
 *
 * It lives in `:domain` because the three callers sit in two modules — `OverpassClient`
 * and `WikipediaClient` in `:data`, `AppAsyncImage` in `:shared` — and a contact address
 * kept in three copies is three places to miss when it changes. That is not theoretical:
 * all three carried the repository's old name for a while after it was renamed.
 *
 * The version is deliberately major.minor rather than the full `saola.versionName`. This
 * string cannot read the build's version — `:domain` is pure Kotlin with no build config —
 * so a patch release would silently leave it behind. A coarse number that stays true beats
 * a precise one that stops being true.
 */
const val SAOLA_USER_AGENT: String =
    "Saola/1.0 (+https://github.com/Evora-Technologies-Global/E01-Saola-AI_Travel_Guide_VietNam; beedyto@gmail.com)"
