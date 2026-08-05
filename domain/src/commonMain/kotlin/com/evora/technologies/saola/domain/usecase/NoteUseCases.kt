package com.evora.technologies.saola.domain.usecase

import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.domain.repository.NoteRepository
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

class ObserveNoteUseCase(
    private val repository: NoteRepository,
) {
    operator fun invoke(discoveryId: String): Flow<DiscoveryNote?> =
        repository.observeNote(discoveryId)
}

/** Saves the traveller's note; an empty one removes the note rather than storing a blank. */
class SaveNoteUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(
        discoveryId: String,
        body: String,
        photoPaths: List<String>,
    ): AppResult<Unit> = repository.save(
        discoveryId = discoveryId,
        body = body,
        photoPaths = photoPaths.take(DiscoveryNote.MAX_PHOTOS),
    )
}

class DeleteNoteUseCase(
    private val repository: NoteRepository,
) {
    suspend operator fun invoke(discoveryId: String): AppResult<Unit> =
        repository.delete(discoveryId)
}
