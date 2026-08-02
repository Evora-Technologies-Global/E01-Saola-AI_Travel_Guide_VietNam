package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.DiscoveryNote
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.flow.Flow

/**
 * The traveller's own writing, kept beside the discovery it belongs to.
 *
 * Separate from [DiscoveryRepository] for the same reason [ChatRepository] is: a note is
 * written long after recognition finished, on a screen that is already showing the
 * discovery, and nothing about saving one should be able to fail a capture.
 */
interface NoteRepository {

    fun observeNote(discoveryId: String): Flow<DiscoveryNote?>

    /**
     * Writes the note, or deletes it if nothing is left of it.
     *
     * @param photoPaths absolute paths already inside app storage — the caller imports
     *   through [CaptureStore] before getting here, so a note never points at a picker
     *   URI that expires. Photos dropped from an edit are deleted from disk.
     */
    suspend fun save(
        discoveryId: String,
        body: String,
        photoPaths: List<String>,
    ): AppResult<Unit>

    suspend fun delete(discoveryId: String): AppResult<Unit>
}
