package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.dao.TranslationDao
import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.data.mapper.toEntity
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.model.TranslationResult
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.repository.TextRecognizer
import com.duylt.trave.vietlensai.domain.repository.TranslationRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class TranslationRepositoryImpl(
    private val translationDao: TranslationDao,
    private val remote: GeminiRemoteDataSource,
    private val textRecognizer: TextRecognizer,
    private val captureStore: CaptureStore,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : TranslationRepository {

    /**
     * Two passes, on purpose: the device finds the text, the model translates it.
     *
     * Splitting them is what makes the overlay possible. Recognition happens where
     * the pixels are, so every line comes back with the box it was read from; the
     * model then only has to turn strings into strings, which it cannot get
     * spatially wrong. It also fails better — a photo with no text never reaches
     * the network at all.
     */
    override suspend fun translate(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
        targetLanguage: TranslateLanguage,
    ): AppResult<TranslationResult> = withContext(ioDispatcher) {
        val recognized = when (val read = textRecognizer.recognize(captureStore.resolve(imagePath), sourceLanguage)) {
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }

        // No legible text at all is a different problem from a failed request, and
        // it is settled here without spending a call to say so.
        if (recognized.isEmpty()) {
            return@withContext AppResult.Failure(AppError.NotRecognized(null))
        }

        val settings = settingsRepository.current()
        val response = when (
            val result = remote.translateLines(
                lines = recognized.map { it.text },
                source = sourceLanguage,
                target = targetLanguage,
                model = settings.preferredModel,
            )
        ) {
            is AppResult.Failure -> return@withContext result
            is AppResult.Success -> result.data
        }

        val entity = response.value.toEntity(
            id = Uuid.random().toString(),
            // Stored as a name; see `CaptureStore.nameOf`.
            imageName = captureStore.nameOf(imagePath),
            recognized = recognized,
            target = targetLanguage,
            createdAt = Clock.System.now(),
        )
        val stored = runCatchingStorage(what = "persist the translation") {
            translationDao.upsert(entity)
        }
        if (stored is AppResult.Failure) return@withContext stored
        AppResult.Success(entity.toDomain(captureStore::resolve))
    }

    override fun observeTranslations(): Flow<List<TranslationResult>> =
        translationDao.observeAll()
            .map { entities -> entities.map { it.toDomain(captureStore::resolve) } }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(emptyList(), what = "observe translations")

    override fun observeTranslation(id: String): Flow<TranslationResult?> =
        translationDao.observeById(id)
            .map { it?.toDomain(captureStore::resolve) }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(null, what = "observe translation $id")

    override suspend fun delete(id: String): AppResult<Unit> = withContext(ioDispatcher) {
        runCatchingStorage(what = "delete translation $id") {
            translationDao.deleteById(id)
        }
    }
}
