package com.evora.technologies.saola.domain.repository

/**
 * Removes capture files that nothing in the database points at any more.
 *
 * Two things hand out capture paths before anything owns them — the lens, which writes a
 * JPEG and only persists a row once Gemini answers, and the note composer, which imports a
 * picked photo the moment it is chosen. Either can be abandoned: the traveller backs out of
 * recognition, or closes the screen instead of saving the note. The file is written by then,
 * and no row will ever mention it.
 *
 * Deleting on every one of those paths individually is possible but not sufficient — a
 * process death between the write and the row leaves the same orphan with nobody left to
 * run the cleanup. This is the backstop that makes the invariant true regardless: a capture
 * file exists only while a row references it.
 */
interface CaptureMaintenance {

    /**
     * Deletes unreferenced captures and returns how many went.
     *
     * Files younger than the sweep's grace period are left alone whatever the database
     * says, because "no row points at this yet" is also what an in-flight capture looks
     * like — the photo taken two seconds ago and still being recognised.
     */
    suspend fun sweepOrphans(): Int
}
