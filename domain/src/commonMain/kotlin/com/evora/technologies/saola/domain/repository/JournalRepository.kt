package com.evora.technologies.saola.domain.repository

import com.evora.technologies.saola.domain.model.JournalDay
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.domain.model.TripSummary
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/** The travel diary: discoveries grouped by day, with an AI write-up per day. */
interface JournalRepository {

    fun observeJournal(): Flow<List<JournalDay>>

    fun observeStats(): Flow<JournalStats>

    /** Generates (or regenerates) the narrative for one day and stores it. */
    suspend fun generateSummary(date: LocalDate): AppResult<TripSummary>
}
