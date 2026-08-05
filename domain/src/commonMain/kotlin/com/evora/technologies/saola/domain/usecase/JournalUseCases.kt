package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.JournalDay
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.domain.model.TripSummary
import com.evora.technologies.saola.domain.repository.JournalRepository
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

class ObserveJournalUseCase(
    private val repository: JournalRepository,
) {
    operator fun invoke(): Flow<List<JournalDay>> = repository.observeJournal()
}

class ObserveJournalStatsUseCase(
    private val repository: JournalRepository,
) {
    operator fun invoke(): Flow<JournalStats> = repository.observeStats()
}

/** Writes (or rewrites) the AI narrative for one day of the trip. */
class GenerateDaySummaryUseCase(
    private val repository: JournalRepository,
) {
    suspend operator fun invoke(date: LocalDate): AppResult<TripSummary> =
        repository.generateSummary(date)
}
