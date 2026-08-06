package com.evora.technologies.saola.domain.model

import kotlin.time.Instant

/**
 * The traveller's objection to what the app said about a discovery.
 *
 * Everything on the result page except the note is a model's account of a place, and a model
 * is confidently wrong in a way a guidebook is not. The page's footer already says which model
 * answered and when; this is the other half of that honesty — a way to say *that answer is
 * wrong* without having to delete the record to be rid of it.
 *
 * **One report per discovery, replaced rather than appended.** A second objection to the same
 * result is the traveller correcting or adding to their first, not a second complaint: a thread
 * of them would need a screen to read it on, and nothing in the app would ever show it. That is
 * also what lets the page state plainly that it has been reported, which is the only feedback
 * a report can honestly give when there is no server to acknowledge it.
 *
 * [createdAt] is therefore the time of the *latest* objection, unlike [DiscoveryNote.createdAt]
 * which deliberately survives a rewrite. A note is a memory and keeps its first date; a report
 * is a claim about a result, and an old date beside a fresh claim would misdate the complaint.
 */
data class DiscoveryReport(
    val discoveryId: String,
    val reason: ReportReason,
    /** What the traveller typed, already trimmed and capped by `SubmitReportUseCase`. */
    val note: String,
    val createdAt: Instant,
) {
    companion object {
        /**
         * Long enough for the correction, short enough to stay one.
         *
         * A report that runs past this is a conversation, and the app already has one of
         * those — the guide is a tap away on the same page.
         */
        const val MAX_NOTE_LENGTH = 500
    }
}

/**
 * What kind of wrong the result is.
 *
 * Four rather than a free-text field alone, because the three specific ones ask for different
 * fixes: a misidentification is a recognition problem, a wrong date inside an otherwise correct
 * story is a generation problem, and something offensive is a safety problem. A single "it's
 * wrong" box would need every report read before it could be sorted.
 *
 * Stored by [name], so these constants are a persistence format: renaming one silently
 * reclassifies every report already on the device as [OTHER] — see `ReportRepositoryImpl`.
 */
enum class ReportReason {
    /** Recognised as the wrong thing entirely — a different temple, a different dish. */
    WRONG_NAME,

    /** The right subject, but something in the story about it is untrue. */
    WRONG_FACTS,

    /** Offensive, or not something the app should have written. */
    INAPPROPRIATE,

    /** Anything the three above do not fit; the note carries the whole of it. */
    OTHER,
}
