package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.local.db.dao.ChatDao
import com.evora.technologies.saola.data.mapper.toDomain
import com.evora.technologies.saola.data.mapper.toEntity
import com.evora.technologies.saola.data.remote.gemini.GeminiRemoteDataSource
import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.model.ChatRole
import com.evora.technologies.saola.domain.model.GeminiModel
import com.evora.technologies.saola.domain.repository.ChatRepository
import com.evora.technologies.saola.domain.repository.DiscoveryRepository
import com.evora.technologies.saola.domain.repository.SettingsRepository
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * The follow-up conversation about one discovery.
 *
 * The question is persisted *before* the model is called, so a failed or
 * interrupted request leaves the traveller's own words on screen instead of
 * silently swallowing them. If the call then fails, the orphaned question is
 * rolled back — otherwise the thread would accumulate unanswered turns that get
 * replayed as context on every future request.
 */
internal class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val remote: GeminiRemoteDataSource,
    private val discoveryRepository: DiscoveryRepository,
    private val settingsRepository: SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : ChatRepository {

    override fun observeMessages(discoveryId: String): Flow<List<ChatMessage>> =
        chatDao.observeThread(discoveryId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
            .fallbackOnFailure(emptyList(), what = "observe the thread for $discoveryId")

    override suspend fun ask(discoveryId: String, question: String): AppResult<ChatMessage> =
        withContext(ioDispatcher) {
            val discovery = discoveryRepository.getDiscovery(discoveryId)
                ?: return@withContext AppResult.Failure(
                    AppError.Storage("No discovery with id $discoveryId"),
                )

            // Guarded like the inserts below. Everything the model is about to be told
            // comes out of this read, so a thread that cannot be loaded has to stop the
            // request rather than quietly send it with no context at all.
            val history = when (
                val read = runCatchingStorage(what = "read the thread for $discoveryId") {
                    chatDao.getThread(discoveryId).map { it.toDomain() }
                }
            ) {
                is AppResult.Failure -> return@withContext read
                is AppResult.Success -> read.data
            }
            val settings = settingsRepository.current()

            val userMessage = ChatMessage(
                id = Uuid.random().toString(),
                discoveryId = discoveryId,
                role = ChatRole.USER,
                content = question,
                createdAt = Clock.System.now(),
            )
            val asked = runCatchingStorage(what = "persist the question") {
                chatDao.insert(userMessage.toEntity())
            }
            if (asked is AppResult.Failure) return@withContext asked

            val response = when (
                val result = remote.chat(
                    discovery = discovery,
                    history = history,
                    question = question,
                    language = settings.language,
                    model = GeminiModel.CONFIGURED,
                )
            ) {
                is AppResult.Failure -> {
                    // The rollback is best-effort and must not replace the model's own
                    // error with a storage one — an orphaned question is a cosmetic
                    // problem, and the traveller is waiting to hear why the answer failed.
                    runCatchingStorage(what = "roll back the orphaned question") {
                        chatDao.deleteById(userMessage.id)
                    }
                    return@withContext result
                }
                is AppResult.Success -> result.data
            }

            val answer = ChatMessage(
                id = Uuid.random().toString(),
                discoveryId = discoveryId,
                role = ChatRole.ASSISTANT,
                content = response.value,
                createdAt = Clock.System.now(),
            )
            val stored = runCatchingStorage(what = "persist the answer") {
                chatDao.insert(answer.toEntity())
            }
            if (stored is AppResult.Failure) return@withContext stored
            AppResult.Success(answer)
        }

    override suspend fun clearThread(discoveryId: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            runCatchingStorage(what = "clear the thread for $discoveryId") {
                chatDao.deleteThread(discoveryId)
            }
        }
}
