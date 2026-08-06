package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * What the traveller has objected to, kept beside the discovery it objects about.
 *
 * Separate from [DiscoveryRepository] for the reason [NoteRepository] is: a report is written
 * long after recognition finished, and nothing about filing one should be able to fail a
 * capture. Separate from [NoteRepository] because the two say opposite things about the same
 * record — a note is the traveller's own memory kept *with* the result, a report is a claim
 * that the result is wrong.
 *
 * **This stores; it does not send.** There is no server to send to, and pretending otherwise
 * inside a repository would make the failure invisible: the app hands the finished report to
 * the system share sheet, where the traveller can see it leave and choose what it leaves in.
 * What the row on disk is for is the page being able to say it has already been reported —
 * without it the button would invite the same objection again on every visit.
 */
interface ReportRepository {

    fun observeReport(discoveryId: String): Flow<DiscoveryReport?>

    /**
     * Writes the objection, replacing any earlier one for the same discovery.
     *
     * @return the stored report, so the caller shares exactly what was written rather than
     *   assembling a second copy of it. The two would differ by the timestamp alone, which is
     *   precisely the field a reader of the mail would use to match it against this device.
     */
    suspend fun submit(
        discoveryId: String,
        reason: ReportReason,
        note: String,
    ): AppResult<DiscoveryReport>
}
