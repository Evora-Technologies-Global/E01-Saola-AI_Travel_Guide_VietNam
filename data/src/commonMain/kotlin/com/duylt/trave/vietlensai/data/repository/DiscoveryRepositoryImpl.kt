package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.file.ImagePolicy
import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.data.mapper.toEntity
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.ProvinceRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Recognition, backed by Room as the single source of truth.
 *
 * [recognize] writes before it returns, and the returned [Discovery] is read back
 * out of the database rather than built in memory. That costs one extra query and
 * removes a whole class of bug where the result screen shows something subtly
 * different from what the journal will show a second later.
 */
internal class DiscoveryRepositoryImpl(
    private val discoveryDao: DiscoveryDao,
    private val noteDao: NoteDao,
    private val remote: GeminiRemoteDataSource,
    private val captureStore: CaptureStore,
    private val settingsRepository: SettingsRepository,
    private val provinceRepository: ProvinceRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : DiscoveryRepository {

    override suspend fun recognize(
        imagePath: String,
        mode: LensMode,
        location: GeoPoint?,
    ): AppResult<Discovery> = withContext(ioDispatcher) {
        val settings = settingsRepository.current()

        val image = when (val read = captureStore.read(imagePath)) {
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }

        val response = when (
            val result = remote.recognize(
                imageBytes = image.bytes,
                mimeType = ImagePolicy.MIME_JPEG,
                mode = mode,
                location = location,
                language = settings.language,
                model = settings.preferredModel,
            )
        ) {
            is AppResult.Failure -> return@withContext result
            is AppResult.Success -> result.data
        }

        val payload = response.value
        if (!payload.recognized || payload.title.isBlank()) {
            return@withContext AppResult.Failure(AppError.NotRecognized(payload.notRecognizedHint))
        }

        // Resolving the province here is what turns a capture into a passport stamp,
        // with no separate check-in for the traveller to remember. It must never cost
        // them the discovery itself, so a failure downgrades to an unstamped row.
        //
        // Spelled out rather than left as the `runCatching` it was: that swallows the
        // CancellationException raised when the traveller backs out of the lens, which
        // would report the abandoned capture as a province lookup that went wrong.
        val provinceId = location?.let { fix ->
            try {
                provinceRepository.provinceAt(fix)?.id
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Could not resolve province for $fix" }
                null
            }
        }

        val entity = payload.toEntity(
            id = Uuid.random().toString(),
            imagePath = imagePath,
            location = location,
            provinceId = provinceId,
            modelUsed = response.modelUsed,
            createdAt = Clock.System.now(),
        )

        // Write, read back and map inside one guard. Only the write used to be wrapped,
        // which left the read-back and `toDomain` outside it — and `toDomain` decodes four
        // JSON columns, so a row written by a schema this version has moved past came out
        // of a method that returns AppResult as a SerializationException instead.
        val stored = runCatchingStorage(what = "persist discovery ${entity.id}") {
            discoveryDao.upsert(entity)
            discoveryDao.getById(entity.id)?.toDomain()
        }
        when (stored) {
            is AppResult.Failure -> stored
            is AppResult.Success -> stored.data?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Storage("Discovery vanished after write"))
        }
    }

    override fun observeDiscoveries(): Flow<List<Discovery>> =
        discoveryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .fallbackOnFailure(emptyList(), what = "observe discoveries")

    override fun observeFavorites(): Flow<List<Discovery>> =
        discoveryDao.observeFavorites()
            .map { entities -> entities.map { it.toDomain() } }
            .fallbackOnFailure(emptyList(), what = "observe favourites")

    override fun observeByProvince(provinceId: String): Flow<List<Discovery>> =
        discoveryDao.observeByProvince(provinceId)
            .map { entities -> entities.map { it.toDomain() } }
            .fallbackOnFailure(emptyList(), what = "observe discoveries in $provinceId")

    override fun observeDiscovery(id: String): Flow<Discovery?> =
        discoveryDao.observeById(id)
            .map { it?.toDomain() }
            .fallbackOnFailure(null, what = "observe discovery $id")

    /**
     * Null covers both "no such row" and "the row could not be read".
     *
     * The signature has no third answer, and the callers all treat null the same way —
     * [ChatRepositoryImpl] turns it into an [AppError.Storage] the chat screen renders.
     */
    override suspend fun getDiscovery(id: String): Discovery? = withContext(ioDispatcher) {
        runCatchingStorageOr(what = "read discovery $id", fallback = null) {
            discoveryDao.getById(id)?.toDomain()
        }
    }

    override suspend fun toggleFavorite(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        runCatchingStorage(what = "toggle the favourite flag on $id") {
            discoveryDao.toggleFavorite(id)
        }
    }

    /**
     * Deleting the row also deletes the capture; an orphaned JPEG helps nobody.
     *
     * The note row goes with it through the foreign key, but SQLite cannot delete the
     * photos the traveller attached to it — so those paths are read out here, before the
     * cascade takes the only record of where they were.
     */
    override suspend fun delete(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        runCatchingStorage(what = "delete discovery $id") {
            val imagePath = discoveryDao.getById(id)?.imagePath
            val notePhotos = noteDao.getByDiscovery(id)?.photoPaths().orEmpty()
            discoveryDao.deleteById(id)
            captureStore.delete(imagePath)
            notePhotos.forEach { captureStore.delete(it) }
        }
    }

    override suspend fun deleteAll(): AppResult<Unit> = withContext(ioDispatcher) {
        runCatchingStorage(what = "delete every discovery") {
            discoveryDao.getRecent(Int.MAX_VALUE).forEach { captureStore.delete(it.imagePath) }
            noteDao.getAll().forEach { note -> note.photoPaths().forEach { captureStore.delete(it) } }
            discoveryDao.deleteAll()
        }
    }
}
