package com.duylt.trave.vietlensai.domain.model

import kotlin.time.Instant

/**
 * What the traveller themselves wrote about a discovery, and the photos they kept with it.
 *
 * Everything else in the journal is Gemini's account of a place. This is the only record
 * the traveller authors, which is what turns the journal from a log of what was recognised
 * into a diary of what was lived — the selfie in front of the mausoleum, and the line about
 * how hot it was standing there.
 *
 * One note per discovery rather than a thread: a second visit means a second photo, and a
 * second photo is already a new [Discovery] to hang a note off. Editing replaces in place,
 * so [createdAt] keeps saying when the memory was first written down even after a rewrite.
 */
data class DiscoveryNote(
    val discoveryId: String,
    val body: String,
    val photoPaths: List<String>,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    val isEmpty: Boolean get() = body.isBlank() && photoPaths.isEmpty()

    val wasEdited: Boolean get() = updatedAt > createdAt

    companion object {
        /**
         * Enough for a moment, few enough to stay a note.
         *
         * The cap is a product decision, not a storage one: past half a dozen shots this
         * stops being a diary entry and becomes an album, which is a different screen.
         */
        const val MAX_PHOTOS = 6
    }
}
