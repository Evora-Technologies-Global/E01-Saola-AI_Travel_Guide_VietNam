package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.RecommendationDao
import com.duylt.trave.vietlensai.data.mapper.toDomain
import com.duylt.trave.vietlensai.data.mapper.toEntity
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.Recommendation
import com.duylt.trave.vietlensai.domain.repository.RecommendationRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * "Where to next", generated from the trip so far and cached.
 *
 * The cache window exists because a full model call per screen visit is both slow
 * and wasteful when the answer barely moves: standing in the same street half an
 * hour later should not cost another request. Pull-to-refresh bypasses it.
 */
internal class RecommendationRepositoryImpl(
    private val recommendationDao: RecommendationDao,
    private val discoveryDao: DiscoveryDao,
    private val remote: GeminiRemoteDataSource,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : RecommendationRepository {

    override fun observeRecommendations(): Flow<List<Recommendation>> =
        recommendationDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .fallbackOnFailure(emptyList(), what = "observe recommendations")

    override suspend fun refresh(
        location: GeoPoint?,
        forceRefresh: Boolean,
    ): AppResult<List<Recommendation>> = withContext(ioDispatcher) {
        // The freshness probe and the cached rows are one guarded read, because they are
        // one question — "is there still a good answer on disk" — and neither half is
        // worth reporting separately.
        val cached = when (
            val read = runCatchingStorage(what = "read the cached recommendations") {
                if (forceRefresh || !isCacheFresh()) null
                else recommendationDao.observeAll().first().map { it.toDomain() }
            }
        ) {
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }
        cached?.let { return@withContext AppResult.Success(it) }

        val recent = when (
            val read = runCatchingStorage(what = "read the recent discoveries") {
                discoveryDao.getRecent(MAX_CONTEXT_DISCOVERIES).map { it.toDomain() }
            }
        ) {
            // Reported rather than downgraded to an empty history. An unreadable table
            // would otherwise look identical to a traveller who has photographed nothing,
            // and the branch below turns that into `LocationUnavailable` — an error about
            // GPS, shown to someone whose database is the thing that is broken.
            is AppResult.Failure -> return@withContext read
            is AppResult.Success -> read.data
        }
        if (recent.isEmpty() && location == null) {
            return@withContext AppResult.Failure(AppError.LocationUnavailable)
        }

        val settings = settingsRepository.current()
        val response = when (
            val result = remote.recommendations(
                location = location,
                visitedPlaces = recent.map { discovery ->
                    listOfNotNull(discovery.title, discovery.placeHint).joinToString(", ")
                },
                // Tags are the cheapest available signal of taste: someone whose photos
                // keep coming back tagged "ceramics" wants different suggestions to
                // someone whose photos are all street food.
                interests = recent.flatMap { it.tags }.distinct(),
                language = settings.language,
                model = settings.preferredModel,
            )
        ) {
            is AppResult.Failure -> return@withContext result
            is AppResult.Success -> result.data
        }

        val generatedAt = Clock.System.now()
        val entities = response.value.recommendations
            .filter { it.name.isNotBlank() }
            .map { it.toEntity(id = Uuid.random().toString(), generatedAt = generatedAt) }

        if (entities.isEmpty()) {
            return@withContext AppResult.Failure(AppError.NotRecognized(null))
        }

        val stored = runCatchingStorage(what = "persist the recommendations") {
            recommendationDao.replaceAll(entities)
        }
        if (stored is AppResult.Failure) return@withContext stored
        AppResult.Success(entities.map { it.toDomain() })
    }

    private suspend fun isCacheFresh(): Boolean {
        val lastGenerated = recommendationDao.lastGeneratedAt() ?: return false
        return Clock.System.now().toEpochMilliseconds() - lastGenerated < CACHE_TTL_MILLIS
    }

    private companion object {
        const val MAX_CONTEXT_DISCOVERIES = 30
        const val CACHE_TTL_MILLIS = 30 * 60 * 1000L
    }
}
