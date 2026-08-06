package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.repository.ReportRepository
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

class ObserveReportUseCase(
    private val repository: ReportRepository,
) {
    operator fun invoke(discoveryId: String): Flow<DiscoveryReport?> =
        repository.observeReport(discoveryId)
}

/**
 * Files the traveller's objection, having first made the note fit.
 *
 * The trim and the cap are here rather than in the composer for the same reason
 * [SaveNoteUseCase] caps its photos here: a limit enforced only by the field that collects the
 * text is a limit the next caller does not have. The sheet still shows a counter, because a
 * silent truncation at the moment of sending would drop the end of a sentence the traveller
 * watched themselves type.
 */
class SubmitReportUseCase(
    private val repository: ReportRepository,
) {
    suspend operator fun invoke(
        discoveryId: String,
        reason: ReportReason,
        note: String,
    ): AppResult<DiscoveryReport> = repository.submit(
        discoveryId = discoveryId,
        reason = reason,
        note = note.trim().take(DiscoveryReport.MAX_NOTE_LENGTH),
    )
}
