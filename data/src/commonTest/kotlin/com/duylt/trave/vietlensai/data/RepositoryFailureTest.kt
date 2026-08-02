package com.duylt.trave.vietlensai.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.duylt.trave.vietlensai.data.local.asset.BundledAssets
import com.duylt.trave.vietlensai.data.local.asset.ProvinceAssetSource
import com.duylt.trave.vietlensai.data.local.datastore.SettingsDataStore
import com.duylt.trave.vietlensai.data.local.db.dao.ChatDao
import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.dao.ProvinceStampRow
import com.duylt.trave.vietlensai.data.local.db.dao.RecommendationDao
import com.duylt.trave.vietlensai.data.local.db.dao.TranslationDao
import com.duylt.trave.vietlensai.data.local.db.dao.TripSummaryDao
import com.duylt.trave.vietlensai.data.local.db.dao.UnstampedRow
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.local.db.entity.RecommendationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity
import com.duylt.trave.vietlensai.data.remote.gemini.ApiKeyProvider
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiClient
import com.duylt.trave.vietlensai.data.remote.gemini.GeminiRemoteDataSource
import com.duylt.trave.vietlensai.data.repository.CaptureMaintenanceImpl
import com.duylt.trave.vietlensai.data.repository.ChatRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.DiscoveryRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.JournalRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.NoteRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.ProvinceRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.RecommendationRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.SettingsRepositoryImpl
import com.duylt.trave.vietlensai.data.repository.TranslationRepositoryImpl
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.CaptureImage
import com.duylt.trave.vietlensai.domain.model.RecognizedLine
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.model.TranslateLanguage
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.TextRecognizer
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondBadRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import androidx.datastore.core.IOException as DataStoreIOException
import okio.IOException as OkioIOException

/**
 * What every repository in `:data` does when the storage underneath it fails.
 *
 * `AppResult` states the contract in its own KDoc — "repositories never throw for expected
 * conditions" — and until this suite existed, only the *writes* kept it. A read that threw
 * escaped as a `SQLiteException`, and a Flow that threw took its collector with it, leaving
 * the screen permanently blank with no way to retry. Neither is something a caller has a
 * branch for, because the type says it cannot happen.
 *
 * So every test below asserts one of three things, and nothing else:
 *
 * 1. a failing call comes back as [AppResult.Failure], not as a throw;
 * 2. a failing Flow emits a safe empty value and stays alive;
 * 3. a [CancellationException] is *not* converted — it is how a coroutine is told to stop,
 *    and a guard that swallows it breaks structured concurrency and turns leaving a screen
 *    into a spurious error.
 *
 * The fakes are hand-written rather than mocked: MockK is JVM-only and this source set also
 * compiles for iOS.
 */
class RepositoryFailureTest {

    /**
     * What a broken database throws, near enough.
     *
     * The real thing is a `SQLiteException` from `androidx.sqlite`, which cannot be
     * constructed in common code. What is being tested is the shape — an ordinary
     * `Exception` out of a DAO call — not the class, and every guard here catches
     * `Exception`.
     */
    private val diskFailure: () -> Throwable =
        { IllegalStateException("database disk image is malformed") }

    // ---------------------------------------------------------------------------------
    // Notes — the read on line 51 that used to sit outside its own try block
    // ---------------------------------------------------------------------------------

    @Test
    fun `saving a note over an unreadable row fails rather than throws`() = runTest {
        val repository = NoteRepositoryImpl(
            noteDao = FakeNoteDao(failure = diskFailure),
            captureStore = FakeCaptureStore(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        val result = repository.save("discovery-1", "Bún chả ở Hàng Mành", emptyList())

        assertStorageFailure(result)
    }

    @Test
    fun `deleting a note that cannot be read fails rather than throws`() = runTest {
        val repository = NoteRepositoryImpl(
            noteDao = FakeNoteDao(failure = diskFailure),
            captureStore = FakeCaptureStore(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertStorageFailure(repository.delete("discovery-1"))
        // The blank-note path deletes through the same helper, so it is asserted too.
        assertStorageFailure(repository.save("discovery-1", "   ", emptyList()))
    }

    @Test
    fun `a note flow that fails emits nothing rather than killing the screen`() = runTest {
        val repository = NoteRepositoryImpl(
            noteDao = FakeNoteDao(failure = diskFailure),
            captureStore = FakeCaptureStore(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertNull(repository.observeNote("discovery-1").first())
    }

    // ---------------------------------------------------------------------------------
    // Discoveries
    // ---------------------------------------------------------------------------------

    @Test
    fun `every discovery flow survives a query that fails`() = runTest {
        val repository = discoveryRepository(FakeDiscoveryDao(failure = diskFailure))

        assertEquals(emptyList(), repository.observeDiscoveries().first())
        assertEquals(emptyList(), repository.observeFavorites().first())
        assertEquals(emptyList(), repository.observeByProvince("01").first())
        assertNull(repository.observeDiscovery("discovery-1").first())
    }

    /**
     * The signature has no failure channel, so "unreadable" has to read as "absent" — and
     * [ChatRepositoryImpl] turns that null into an [AppError.Storage] the chat can render.
     */
    @Test
    fun `a discovery that cannot be read comes back absent`() = runTest {
        val repository = discoveryRepository(FakeDiscoveryDao(failure = diskFailure))

        assertNull(repository.getDiscovery("discovery-1"))
    }

    @Test
    fun `discovery writes report a storage failure`() = runTest {
        val repository = discoveryRepository(FakeDiscoveryDao(failure = diskFailure))

        assertStorageFailure(repository.toggleFavorite("discovery-1"))
        assertStorageFailure(repository.delete("discovery-1"))
        assertStorageFailure(repository.deleteAll())
    }

    // ---------------------------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------------------------

    @Test
    fun `a chat thread that fails to load reports rather than throws`() = runTest {
        val repository = ChatRepositoryImpl(
            chatDao = FakeChatDao(failure = diskFailure),
            remote = unusedRemote(),
            // A discovery that reads back cleanly, deliberately: `ask` has to get past it
            // to reach the history read, which is the call that used to be unguarded. With
            // a failing discovery too, the test would pass on the wrong branch.
            discoveryRepository = discoveryRepository(
                FakeDiscoveryDao(rows = listOf(discoveryEntity("discovery-1"))),
            ),
            settingsRepository = workingSettings(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(emptyList(), repository.observeMessages("discovery-1").first())
        assertStorageFailure(repository.clearThread("discovery-1"))
        // The history read fails, so the question is never sent — which is what makes the
        // refusing engine behind `unusedRemote` safe here.
        assertStorageFailure(repository.ask("discovery-1", "Xây năm nào?"))
    }

    // ---------------------------------------------------------------------------------
    // Journal — including a row this version can no longer parse
    // ---------------------------------------------------------------------------------

    /**
     * Not a thrown DAO, a *stored* value: `TripSummaryEntity.toDomain` runs
     * `LocalDate.parse` on the primary key, so a `date` column written by anything but this
     * version's formatter throws inside the mapper — after Room has already answered.
     *
     * That is the exact shape the mapper guard exists for, and this is the app's home
     * screen: without it the journal is blank from launch until the app is reinstalled.
     */
    @Test
    fun `the journal survives a summary row whose date no longer parses`() = runTest {
        val discoveries = listOf(discoveryEntity("discovery-1"))

        // The control, and the reason this test means anything: the very same day, with a
        // date column this version can read, comes back as one journal day. Without it,
        // "empty" below would be indistinguishable from a journal that was empty anyway.
        val readable = journalRepository(
            discoveryDao = FakeDiscoveryDao(rows = discoveries),
            summaryDao = FakeTripSummaryDao(rows = listOf(summaryEntity(date = "2026-08-02"))),
        )
        assertEquals(1, readable.observeJournal().first().size)

        val corrupt = journalRepository(
            discoveryDao = FakeDiscoveryDao(rows = discoveries),
            summaryDao = FakeTripSummaryDao(rows = listOf(summaryEntity(date = "sometime in May"))),
        )
        assertEquals(emptyList(), corrupt.observeJournal().first())
    }

    @Test
    fun `journal statistics fall back to zero when the query fails`() = runTest {
        val repository = journalRepository(
            discoveryDao = FakeDiscoveryDao(failure = diskFailure),
            summaryDao = FakeTripSummaryDao(failure = diskFailure),
        )

        val stats = repository.observeStats().first()

        assertEquals(0, stats.totalDiscoveries)
        assertEquals(0, stats.totalDays)
        assertEquals(emptyMap(), stats.categoryBreakdown)
        assertEquals(emptyList(), repository.observeJournal().first())
    }

    @Test
    fun `generating a summary over an unreadable day fails rather than throws`() = runTest {
        val repository = journalRepository(
            discoveryDao = FakeDiscoveryDao(failure = diskFailure),
            summaryDao = FakeTripSummaryDao(),
        )

        assertStorageFailure(repository.generateSummary(LocalDate(2026, 8, 2)))
    }

    // ---------------------------------------------------------------------------------
    // Translations and recommendations
    // ---------------------------------------------------------------------------------

    @Test
    fun `translation reads and deletes survive a failing table`() = runTest {
        val repository = TranslationRepositoryImpl(
            translationDao = FakeTranslationDao(failure = diskFailure),
            remote = unusedRemote(),
            textRecognizer = object : TextRecognizer {
                override suspend fun recognize(
                    imagePath: String,
                    sourceLanguage: TranslateLanguage?,
                ): AppResult<List<RecognizedLine>> = AppResult.Failure(AppError.NotRecognized(null))
            },
            settingsRepository = workingSettings(),
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(emptyList(), repository.observeTranslations().first())
        assertNull(repository.observeTranslation("translation-1").first())
        assertStorageFailure(repository.delete("translation-1"))
    }

    @Test
    fun `a failing recommendation cache is a storage failure, not a missing location`() =
        runTest {
            val repository = RecommendationRepositoryImpl(
                recommendationDao = FakeRecommendationDao(failure = diskFailure),
                discoveryDao = FakeDiscoveryDao(failure = diskFailure),
                remote = unusedRemote(),
                settingsRepository = workingSettings(),
                ioDispatcher = Dispatchers.Unconfined,
            )

            assertEquals(emptyList(), repository.observeRecommendations().first())
            // Specifically not `LocationUnavailable`, which is what an unguarded read used
            // to produce: an empty history and an unreadable one looked identical, so a
            // broken database was reported to the traveller as a GPS problem.
            assertStorageFailure(repository.refresh(location = null, forceRefresh = false))
        }

    // ---------------------------------------------------------------------------------
    // The passport, and the sweep that deletes files
    // ---------------------------------------------------------------------------------

    /**
     * The outlines come from the shipped asset, not from Room, so a failed roll-up costs
     * the counts and leaves the map itself standing.
     *
     * That is why the guard sits *before* the mapping rather than at the end of the chain,
     * and it is what this asserts: the province is still there, with nothing stamped on it.
     * Caught one operator later, the same failure would have emitted an empty passport —
     * a blank map, which is the outcome the repository's own comment forbids.
     */
    @Test
    fun `the passport still draws its provinces when the stamp roll-up fails`() = runTest {
        val repository = provinceRepository(FakeDiscoveryDao(failure = diskFailure))

        val stamp = repository.observePassport().first().stamps.single()

        assertEquals("01", stamp.province.id)
        assertEquals(0, stamp.discoveryCount)
    }

    @Test
    fun `the backfill reports nothing stamped when the database cannot be read`() = runTest {
        val repository = provinceRepository(FakeDiscoveryDao(failure = diskFailure))

        assertEquals(0, repository.backfillProvinces())
    }

    /**
     * The one place where catching an exception would be more dangerous than letting it
     * escape.
     *
     * Every path the reference query fails to contribute looks exactly like an orphan, so a
     * sweep that carried on with a partial answer would delete the photographs behind the
     * traveller's notes. The assertion that matters is the second one: not merely that the
     * sweep returned, but that it touched nothing.
     */
    @Test
    fun `the orphan sweep deletes nothing when it cannot tell what is referenced`() = runTest {
        val captureStore = FakeCaptureStore(
            captures = listOf("/captures/capture_1.jpg", "/captures/capture_2.jpg"),
        )
        val maintenance = CaptureMaintenanceImpl(
            discoveryDao = FakeDiscoveryDao(),
            noteDao = FakeNoteDao(failure = diskFailure),
            translationDao = FakeTranslationDao(),
            captureStore = captureStore,
            ioDispatcher = Dispatchers.Unconfined,
        )

        assertEquals(0, maintenance.sweepOrphans())
        assertEquals(emptyList(), captureStore.deleted)
    }

    // ---------------------------------------------------------------------------------
    // Settings — the corrupt preferences file, in each type it can arrive as
    // ---------------------------------------------------------------------------------

    /**
     * Three unrelated classes called `IOException` reach that `catch`, and only the JVM
     * collapses them into one. This test is written for the platform where it matters: on
     * Apple targets `androidx.datastore.core.IOException` and `okio.IOException` are
     * genuinely different types, and a check that names the wrong one matches neither.
     */
    @Test
    fun `an unreadable preferences file reads back as defaults`() = runTest {
        val failures = listOf<() -> Throwable>(
            { DataStoreIOException("Unable to rename .tmp", null) },
            { OkioIOException("failed to read file") },
        )

        failures.forEach { failure ->
            val settings = SettingsDataStore(FakeDataStore(failure = failure)).settings.first()
            assertEquals(AppSettings.DEFAULT, settings)
        }
    }

    /**
     * Anything that is *not* an IO failure is a bug rather than a corrupt file, so the
     * store deliberately rethrows it — and [SettingsRepositoryImpl] is the floor that keeps
     * it from reaching a caller who has nowhere to put it. `current()` returns
     * `AppSettings`, and recognition, chat and translation all read it before their own
     * guards run.
     */
    @Test
    fun `settings that cannot be read at all still answer with defaults`() = runTest {
        val repository = SettingsRepositoryImpl(
            SettingsDataStore(FakeDataStore(failure = diskFailure)),
        )

        assertEquals(AppSettings.DEFAULT, repository.settings.first())
        assertEquals(AppSettings.DEFAULT, repository.current())
        // Reads `current()` as well, and has to answer rather than throw. Either answer is
        // correct — a build with a key baked into `local.properties` says true, one without
        // says false — so only the returning is asserted.
        repository.hasUsableApiKey()
    }

    @Test
    fun `a settings write that fails is swallowed rather than thrown`() = runTest {
        val store = SettingsDataStore(FakeDataStore(failure = diskFailure))

        // No assertion beyond returning: every one of these is called from a screen that
        // has already moved on, and none of them has an AppResult to report into.
        store.setApiKey("AIza-nope")
        store.setSpeakAnswers(false)
        store.setLocationAsked()
        store.setThemePreference(ThemePreference.DARK)
    }

    // ---------------------------------------------------------------------------------
    // Cancellation is not a failure
    // ---------------------------------------------------------------------------------

    /**
     * `CancellationException` is an `Exception`, so every `catch (e: Exception)` in this
     * package would swallow it without the explicit branch in front — reporting a screen
     * the traveller has already left as a broken database, and quietly breaking structured
     * concurrency on the way.
     */
    @Test
    fun `cancellation is rethrown rather than reported as a storage failure`() = runTest {
        val cancelled: () -> Throwable = { CancellationException("the screen went away") }
        val repository = discoveryRepository(FakeDiscoveryDao(failure = cancelled))

        assertFailsWith<CancellationException> { repository.toggleFavorite("discovery-1") }
        assertFailsWith<CancellationException> { repository.getDiscovery("discovery-1") }
        assertFailsWith<CancellationException> {
            repository.observeDiscoveries().first()
        }
    }

    // ---------------------------------------------------------------------------------
    // Assembly
    // ---------------------------------------------------------------------------------

    private fun assertStorageFailure(result: AppResult<*>) {
        val error = result.errorOrNull()
        assertTrue(
            error is AppError.Storage,
            "expected a rendered storage failure, got $result",
        )
    }

    private fun discoveryRepository(discoveryDao: DiscoveryDao) = DiscoveryRepositoryImpl(
        discoveryDao = discoveryDao,
        noteDao = FakeNoteDao(),
        remote = unusedRemote(),
        captureStore = FakeCaptureStore(),
        settingsRepository = workingSettings(),
        provinceRepository = provinceRepository(discoveryDao),
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun provinceRepository(discoveryDao: DiscoveryDao) = ProvinceRepositoryImpl(
        assetSource = ProvinceAssetSource(BundledAssets { ONE_PROVINCE_ASSET }, Dispatchers.Unconfined),
        discoveryDao = discoveryDao,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun journalRepository(
        discoveryDao: DiscoveryDao,
        summaryDao: TripSummaryDao,
    ) = JournalRepositoryImpl(
        discoveryDao = discoveryDao,
        summaryDao = summaryDao,
        noteDao = FakeNoteDao(),
        remote = unusedRemote(),
        settingsRepository = workingSettings(),
        timeZone = TimeZone.UTC,
        ioDispatcher = Dispatchers.Unconfined,
    )

    private fun workingSettings() = SettingsRepositoryImpl(SettingsDataStore(FakeDataStore()))

    /**
     * Wired to an engine that refuses everything, because no test here should reach it.
     *
     * Each of these repositories fails on a local read long before the prompt is built. If
     * one ever stops doing so, the request comes back as a 400 rather than as a passing
     * test against a network call nobody meant to make.
     */
    private fun unusedRemote(): GeminiRemoteDataSource {
        val json = Json { ignoreUnknownKeys = true }
        return GeminiRemoteDataSource(
            GeminiClient(
                httpClient = HttpClient(MockEngine { respondBadRequest() }) {
                    expectSuccess = false
                    install(ContentNegotiation) { json(json) }
                },
                apiKeyProvider = object : ApiKeyProvider {
                    override suspend fun apiKey(): String = "test-key"
                },
                json = json,
            ),
        )
    }

    private fun summaryEntity(date: String) = TripSummaryEntity(
        date = date,
        headline = "Một ngày ở Hà Nội",
        narrative = "…",
        highlightsJson = "[]",
        tomorrowIdeasJson = "[]",
        generatedAt = 0L,
    )

    private fun discoveryEntity(id: String) = DiscoveryEntity(
        id = id,
        title = "Văn Miếu",
        localName = null,
        category = "ARCHITECTURE",
        imagePath = "/captures/capture_1.jpg",
        summary = "Trường đại học đầu tiên của Việt Nam.",
        sectionsJson = "[]",
        funFactsJson = "[]",
        tagsJson = "[]",
        nearbyJson = "[]",
        suggestedQuestionsJson = "[]",
        confidence = 0.9f,
        latitude = null,
        longitude = null,
        provinceId = null,
        placeHint = null,
        isFavorite = false,
        modelUsed = null,
        createdAt = 0L,
    )

    private companion object {
        /**
         * One province rather than the shipped 34: the passport test only needs to prove
         * that an outline survives a failed stamp query, and parsing 9,228 vertices to
         * show it would tie this suite to the real asset file for no gain.
         */
        val ONE_PROVINCE_ASSET = """
            {"version":1,"provinces":[{
              "id":"01","name":"Hà Nội","bbox":[105.0,20.0,106.0,21.0],
              "polys":[[105.0,20.0,106.0,20.0,106.0,21.0]]
            }]}
        """.trimIndent()
    }
}

// -------------------------------------------------------------------------------------
// Fakes
//
// One class per collaborator, each with a single `failure` switch rather than a
// per-method one: "this table is broken" is how the real failure arrives — the database
// file is unreadable, not one query in it — and a switch per method would let a guard be
// added to two of seven and still look tested.
// -------------------------------------------------------------------------------------

private class FakeDiscoveryDao(
    private val rows: List<DiscoveryEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : DiscoveryDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    private fun <T> answerFlow(value: T): Flow<T> = flow { emit(answer(value)) }

    override fun observeAll(): Flow<List<DiscoveryEntity>> = answerFlow(rows)
    override fun observeFavorites(): Flow<List<DiscoveryEntity>> = answerFlow(rows)
    override fun observeById(id: String): Flow<DiscoveryEntity?> = answerFlow(rows.firstOrNull())
    override fun observeByProvince(provinceId: String): Flow<List<DiscoveryEntity>> =
        answerFlow(rows)

    override fun observeCount(): Flow<Int> = answerFlow(rows.size)
    override fun observeFavoriteCount(): Flow<Int> = answerFlow(0)
    override fun observeProvinceStamps(): Flow<List<ProvinceStampRow>> = answerFlow(emptyList())

    override suspend fun getById(id: String): DiscoveryEntity? = answer(rows.firstOrNull { it.id == id })
    override suspend fun getRecent(limit: Int): List<DiscoveryEntity> = answer(rows)
    override suspend fun getBetween(
        fromEpochMillis: Long,
        toEpochMillis: Long,
    ): List<DiscoveryEntity> = answer(rows)

    override suspend fun getUnstamped(): List<UnstampedRow> = answer(emptyList())
    override suspend fun getAllImagePaths(): List<String> = answer(rows.mapNotNull { it.imagePath })
    override suspend fun upsert(entity: DiscoveryEntity) = answer(Unit)
    override suspend fun toggleFavorite(id: String) = answer(Unit)
    override suspend fun setProvinceId(id: String, provinceId: String?) = answer(Unit)
    override suspend fun deleteById(id: String) = answer(Unit)
    override suspend fun deleteAll() = answer(Unit)
}

private class FakeNoteDao(
    private val rows: List<DiscoveryNoteEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : NoteDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    override fun observeByDiscovery(discoveryId: String): Flow<DiscoveryNoteEntity?> =
        flow { emit(answer(rows.firstOrNull { it.discoveryId == discoveryId })) }

    override fun observeCount(): Flow<Int> = flow { emit(answer(rows.size)) }

    override suspend fun getByDiscovery(discoveryId: String): DiscoveryNoteEntity? =
        answer(rows.firstOrNull { it.discoveryId == discoveryId })

    override suspend fun getForDiscoveries(discoveryIds: List<String>): List<DiscoveryNoteEntity> =
        answer(rows.filter { it.discoveryId in discoveryIds })

    override suspend fun getAll(): List<DiscoveryNoteEntity> = answer(rows)
    override suspend fun upsert(entity: DiscoveryNoteEntity) = answer(Unit)
    override suspend fun deleteByDiscovery(discoveryId: String) = answer(Unit)
}

private class FakeChatDao(
    private val rows: List<ChatMessageEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : ChatDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    override fun observeThread(discoveryId: String): Flow<List<ChatMessageEntity>> =
        flow { emit(answer(rows)) }

    override suspend fun getThread(discoveryId: String): List<ChatMessageEntity> = answer(rows)
    override suspend fun insert(message: ChatMessageEntity) = answer(Unit)
    override suspend fun deleteThread(discoveryId: String) = answer(Unit)
    override suspend fun deleteById(id: String) = answer(Unit)
}

private class FakeTranslationDao(
    private val rows: List<TranslationEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : TranslationDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    override fun observeAll(): Flow<List<TranslationEntity>> = flow { emit(answer(rows)) }
    override fun observeById(id: String): Flow<TranslationEntity?> =
        flow { emit(answer(rows.firstOrNull { it.id == id })) }

    override suspend fun getAllImagePaths(): List<String> = answer(rows.mapNotNull { it.imagePath })
    override suspend fun upsert(entity: TranslationEntity) = answer(Unit)
    override suspend fun deleteById(id: String) = answer(Unit)
    override suspend fun deleteAll() = answer(Unit)
}

private class FakeRecommendationDao(
    private val rows: List<RecommendationEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : RecommendationDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    override fun observeAll(): Flow<List<RecommendationEntity>> = flow { emit(answer(rows)) }
    override suspend fun lastGeneratedAt(): Long? = answer(null)
    override suspend fun insertAll(entities: List<RecommendationEntity>) = answer(Unit)
    override suspend fun deleteAll() = answer(Unit)
}

private class FakeTripSummaryDao(
    private val rows: List<TripSummaryEntity> = emptyList(),
    private val failure: (() -> Throwable)? = null,
) : TripSummaryDao {

    private fun <T> answer(value: T): T = failure?.let { throw it() } ?: value

    override fun observeAll(): Flow<List<TripSummaryEntity>> = flow { emit(answer(rows)) }
    override suspend fun getByDate(date: String): TripSummaryEntity? =
        answer(rows.firstOrNull { it.date == date })

    override suspend fun upsert(entity: TripSummaryEntity) = answer(Unit)
    override suspend fun deleteAll() = answer(Unit)
}

/** Records deletions rather than performing them, so the orphan sweep can be held to account. */
private class FakeCaptureStore(
    private val captures: List<String> = emptyList(),
) : CaptureStore {

    val deleted = mutableListOf<String>()

    override fun newCapturePath(): String = "/captures/capture_0.jpg"

    override suspend fun read(path: String): AppResult<CaptureImage> =
        AppResult.Failure(AppError.ImageUnavailable(path))

    override suspend fun importFromPicker(source: String): AppResult<String> =
        AppResult.Failure(AppError.ImageUnavailable(source))

    override suspend fun delete(path: String?) {
        path?.let { deleted += it }
    }

    override suspend fun flattenOrientation(path: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun listCaptures(): List<String> = captures
}

/** An empty preferences file, or one that cannot be opened at all. */
private class FakeDataStore(
    private val failure: (() -> Throwable)? = null,
) : DataStore<Preferences> {

    override val data: Flow<Preferences> = flow {
        failure?.let { throw it() }
        emit(emptyPreferences())
    }

    override suspend fun updateData(
        transform: suspend (t: Preferences) -> Preferences,
    ): Preferences {
        failure?.let { throw it() }
        return transform(emptyPreferences())
    }
}
