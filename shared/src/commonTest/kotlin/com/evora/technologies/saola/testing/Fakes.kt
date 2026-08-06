package com.evora.technologies.saola.testing

import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.AppSettings
import com.evora.technologies.saola.domain.model.CaptureImage
import com.evora.technologies.saola.domain.model.ChatMessage
import com.evora.technologies.saola.domain.model.ChatRole
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.GeminiModel
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.LensMode
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.evora.technologies.saola.domain.model.PlaceDetails
import com.evora.technologies.saola.domain.model.ThemePreference
import com.evora.technologies.saola.domain.model.CultureCollection
import com.evora.technologies.saola.domain.model.DiscoveryNote
import com.evora.technologies.saola.domain.model.DiscoveryReport
import com.evora.technologies.saola.domain.model.ReportReason
import com.evora.technologies.saola.domain.model.JournalDay
import com.evora.technologies.saola.domain.model.JournalStats
import com.evora.technologies.saola.domain.model.Province
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.domain.model.TranslationBlock
import com.evora.technologies.saola.domain.model.TranslationResult
import com.evora.technologies.saola.domain.model.TravelPassport
import com.evora.technologies.saola.domain.model.TripSummary
import com.evora.technologies.saola.domain.repository.DemoDataSeeder
import com.evora.technologies.saola.domain.repository.CaptureMaintenance
import com.evora.technologies.saola.domain.repository.CatalogRepository
import com.evora.technologies.saola.domain.repository.ChatRepository
import com.evora.technologies.saola.domain.repository.JournalRepository
import com.evora.technologies.saola.domain.repository.NoteRepository
import com.evora.technologies.saola.domain.repository.PlaceRepository
import com.evora.technologies.saola.domain.repository.ProvinceRepository
import com.evora.technologies.saola.domain.repository.ReportRepository
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.repository.DiscoveryRepository
import com.evora.technologies.saola.domain.repository.LocationRepository
import com.evora.technologies.saola.domain.repository.SettingsRepository
import com.evora.technologies.saola.domain.repository.TranslationRepository
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import com.evora.technologies.saola.voice.TextToSpeechManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Tears a ViewModel down the way the framework does, so `onCleared` actually runs.
 *
 * `ViewModel.onCleared` is `protected`, and `clear()` is internal to the lifecycle library,
 * so a test cannot call either directly. Putting the instance into a real [ViewModelStore]
 * and clearing the store is the public route to the same callback — and it is the same one
 * the navigation host takes when a destination leaves the back stack, which is exactly the
 * moment the release logic in `onCleared` has to hold up.
 */
fun ViewModel.clearAsFrameworkWould() {
    ViewModelStore().apply { put("test", this@clearAsFrameworkWould) }.clear()
}

/**
 * Hand-written doubles for the presentation-layer suites.
 *
 * Hand-written rather than mocked for the same reason the domain suite gives: these tests
 * compile for Kotlin/Native as well as the JVM, and MockK is JVM-only. It also makes the
 * one thing these tests are really about — *can this ViewModel be made to throw* — a
 * property of a plainly readable fake rather than of a stubbing DSL.
 *
 * Every fake carries a `throwOnNext…` switch. That is the point of the file: a repository
 * that returns `AppResult.Failure` is the handled path and is already covered elsewhere,
 * while a repository that *throws* is the unhandled one, and an exception escaping a
 * `viewModelScope.launch` takes the whole app down with it.
 */

/** The canonical discovery used wherever a suite needs a well-formed one. */
fun discovery(
    id: String = "d1",
    title: String = "Temple of Literature",
    createdAt: Instant = Instant.fromEpochSeconds(1_700_000_000),
): Discovery = Discovery(
    id = id,
    title = title,
    localName = "Văn Miếu",
    category = DiscoveryCategory.LANDMARK,
    imagePath = "/captures/capture_1.jpg",
    summary = "Vietnam's first university.",
    sections = emptyList(),
    funFacts = emptyList(),
    tags = emptyList(),
    nearbySuggestions = emptyList(),
    suggestedQuestions = emptyList(),
    confidence = 0.9f,
    location = GeoPoint(21.0287, 105.8524),
    placeHint = null,
    isFavorite = false,
    modelUsed = GeminiModel.CONFIGURED.id,
    createdAt = createdAt,
)

class FakeDiscoveryRepository : DiscoveryRepository {

    val discoveries = MutableStateFlow<List<Discovery>>(emptyList())

    /** What [recognize] hands back when it is allowed to return at all. */
    var result: AppResult<Discovery> = AppResult.Success(discovery())

    /**
     * How long [recognize] takes, on the test scheduler's virtual clock.
     *
     * Zero would let a recognition finish inside the same `runCurrent()` that started it,
     * which makes "a second capture cancels the first" untestable — there would never be a
     * first still in flight to cancel.
     */
    var recognizeDelayMillis: Long = 0

    /**
     * Makes [recognize] throw instead of returning.
     *
     * This is not a hypothetical. `DiscoveryRepositoryImpl.recognize` reads DataStore,
     * maps a Gemini payload into a Room entity, and deserialises the row it just wrote —
     * and the read-back and both mappers sit outside its `try`. Any of them can throw on
     * a corrupt preferences file or a payload that decodes into something the mapper does
     * not expect.
     */
    var throwOnRecognize: Throwable? = null

    var recognizeCalls = 0

    override suspend fun recognize(
        imagePath: String,
        mode: LensMode,
        location: GeoPoint?,
    ): AppResult<Discovery> {
        recognizeCalls++
        throwOnRecognize?.let { throw it }
        if (recognizeDelayMillis > 0) delay(recognizeDelayMillis)
        return result
    }

    override fun observeDiscoveries(): Flow<List<Discovery>> = discoveries
    override fun observeFavorites(): Flow<List<Discovery>> = discoveries
    override fun observeDiscovery(id: String): Flow<Discovery?> =
        flowOf(discoveries.value.firstOrNull { it.id == id })

    /**
     * What was captured in each province, published only once a test says so.
     *
     * A province absent from this map emits *nothing* rather than an empty list, and that is
     * the point: Room takes a tick to answer, so the frame between tapping a province and its
     * photographs arriving is a real frame the traveller sees. A fake that answered
     * synchronously would paper over exactly the state worth asserting — the previous
     * province's photographs still on screen under the new province's name.
     */
    val provinceDiscoveries = MutableStateFlow<Map<String, List<Discovery>>>(emptyMap())

    override fun observeByProvince(provinceId: String): Flow<List<Discovery>> =
        provinceDiscoveries.mapNotNull { it[provinceId] }
    override suspend fun getDiscovery(id: String): Discovery? =
        discoveries.value.firstOrNull { it.id == id }

    var throwOnToggleFavorite: Throwable? = null

    // Both spellings of "it did not work", because a ViewModel has to answer both and they
    // are separate code paths: `failOn…` is the ordinary handled failure a repository returns,
    // `throwOn…` the unwrapped one it promises never to produce. Four of this app's silent
    // failures lived on the first of those, which no `throwOn…` hook can reach.
    var failOnToggleFavorite: AppError? = null
    var failOnDelete: AppError? = null
    val deleted = mutableListOf<String>()

    override suspend fun toggleFavorite(id: String): AppResult<Unit> {
        throwOnToggleFavorite?.let { throw it }
        failOnToggleFavorite?.let { return AppResult.Failure(it) }
        return AppResult.Success(Unit)
    }

    override suspend fun delete(id: String): AppResult<Unit> {
        deleted += id
        failOnDelete?.let { return AppResult.Failure(it) }
        discoveries.value = discoveries.value.filterNot { it.id == id }
        return AppResult.Success(Unit)
    }

    // Both spellings again, for the one write on the settings page that reports in words.
    // `deleteAllCalls` counts attempts rather than successes, so a test can assert that a
    // failure was actually reached rather than that nothing happened.
    var failOnDeleteAll: AppError? = null
    var throwOnDeleteAll: Throwable? = null
    var deleteAllCalls = 0

    override suspend fun deleteAll(): AppResult<Unit> {
        deleteAllCalls++
        throwOnDeleteAll?.let { throw it }
        failOnDeleteAll?.let { return AppResult.Failure(it) }
        discoveries.value = emptyList()
        return AppResult.Success(Unit)
    }
}

class FakeLocationRepository : LocationRepository {
    var permission = false
    var fix: AppResult<GeoPoint> = AppResult.Failure(AppError.LocationUnavailable)
    var throwOnCurrentLocation: Throwable? = null

    override suspend fun currentLocation(): AppResult<GeoPoint> {
        throwOnCurrentLocation?.let { throw it }
        return fix
    }

    override fun hasLocationPermission(): Boolean = permission
}

class FakeSettingsRepository : SettingsRepository {

    val state = MutableStateFlow(AppSettings.DEFAULT)

    /** `DataStore` throws `IOException` on a corrupt or unreadable preferences file. */
    var throwOnCurrent: Throwable? = null
    var throwOnWrite: Throwable? = null
    var failOnWrite: AppError? = null

    var usableApiKey = true
    var locationAskedCalls = 0

    override val settings: Flow<AppSettings> = state

    override suspend fun current(): AppSettings {
        throwOnCurrent?.let { throw it }
        return state.value
    }

    override suspend fun hasUsableApiKey(): Boolean {
        throwOnCurrent?.let { throw it }
        return usableApiKey
    }

    override suspend fun setSpeakAnswers(enabled: Boolean) = write {
        state.value = state.value.copy(speakAnswers = enabled)
    }

    /**
     * The counter is bumped before [write], not inside it.
     *
     * `LensViewModelCrashTest.'an exception while recording the location prompt is contained'`
     * asserts the call was *attempted* when the write blows up, so the count has to survive a
     * throw — counting inside the block would only ever record the writes that succeeded.
     */
    override suspend fun setLocationAsked(): AppResult<Unit> {
        locationAskedCalls++
        return write { state.value = state.value.copy(hasAskedLocation = true) }
    }

    override suspend fun setThemePreference(preference: ThemePreference) = write {
        state.value = state.value.copy(darkTheme = preference)
    }

    /**
     * The two ways a write can go wrong, kept apart on purpose.
     *
     * [throwOnWrite] is the crash probe this file exists for — the unhandled path, where the
     * question is whether the caller survives at all. [failOnWrite] is the handled one the
     * repository now promises: a write that reports [AppResult.Failure] instead of throwing,
     * which is what the settings screen reads to decide whether it may say "saved".
     */
    private inline fun write(block: () -> Unit): AppResult<Unit> {
        throwOnWrite?.let { throw it }
        failOnWrite?.let { return AppResult.Failure(it) }
        block()
        return AppResult.Success(Unit)
    }
}

class FakeCaptureStore : CaptureStore {
    var nextPath = "/captures/capture_1.jpg"
    var throwOnRead: Throwable? = null
    val deleted = mutableListOf<String?>()

    override fun newCapturePath(): String = nextPath

    override fun nameOf(nameOrPath: String): String = nameOrPath.substringAfterLast('/')

    override fun resolve(nameOrPath: String): String = "/captures/${nameOf(nameOrPath)}"

    override suspend fun read(path: String): AppResult<CaptureImage> {
        throwOnRead?.let { throw it }
        return AppResult.Success(CaptureImage(bytes = ByteArray(8), widthPx = 4, heightPx = 2))
    }

    override suspend fun importFromPicker(source: String): AppResult<String> =
        AppResult.Success(nextPath)

    override suspend fun delete(path: String?) {
        deleted += path
    }

    override suspend fun flattenOrientation(path: String): AppResult<Unit> = AppResult.Success(Unit)

    override suspend fun listCaptures(): List<String> = emptyList()
}


/**
 * The journal, with both ways a day summary can fail.
 *
 * [failOnGenerate] is the ordinary path — the repository folds the failure into an
 * [AppResult] the way the real one does. [throwOnGenerate] is the crash floor: an
 * unwrapped throw, which is the gap `launchSafely` exists to catch and which used to lower
 * the day's spinner while telling the traveller nothing at all.
 */
class FakeJournalRepository : JournalRepository {
    val days = MutableStateFlow<List<JournalDay>>(emptyList())
    val stats = MutableStateFlow(JournalStats(0, 0, 0, emptyMap()))
    var failOnGenerate: AppError? = null
    var throwOnGenerate: Throwable? = null
    var generateDelayMillis: Long = 0
    var generateCalls = 0

    override fun observeJournal(): Flow<List<JournalDay>> = days
    override fun observeStats(): Flow<JournalStats> = stats

    override suspend fun generateSummary(date: LocalDate): AppResult<TripSummary> {
        generateCalls += 1
        if (generateDelayMillis > 0) delay(generateDelayMillis)
        throwOnGenerate?.let { throw it }
        failOnGenerate?.let { return AppResult.Failure(it) }
        return AppResult.Success(
            TripSummary(
                date = date,
                headline = "A day",
                narrative = "It happened.",
                highlights = emptyList(),
                tomorrowIdeas = emptyList(),
                generatedAt = Instant.fromEpochMilliseconds(0),
            ),
        )
    }
}

/**
 * The follow-up conversation, keyed by discovery so a test can prove which one is being read.
 *
 * [threads] is a map rather than a single list on purpose: the guide column on a large window
 * takes its discovery id from the caller instead of from a route, and the failure that change
 * guards against — the guide answering about the wrong place — is only visible if the fake can
 * hold two conversations at once.
 */
class FakeChatRepository : ChatRepository {

    val threads = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())

    var throwOnAsk: Throwable? = null
    var failOnAsk: AppError? = null
    val askedFor = mutableListOf<Pair<String, String>>()
    val clearedThreads = mutableListOf<String>()

    override fun observeMessages(discoveryId: String): Flow<List<ChatMessage>> =
        threads.map { it[discoveryId].orEmpty() }

    override suspend fun ask(discoveryId: String, question: String): AppResult<ChatMessage> {
        askedFor += discoveryId to question
        throwOnAsk?.let { throw it }
        failOnAsk?.let { return AppResult.Failure(it) }
        val answer = ChatMessage(
            id = "a${askedFor.size}",
            discoveryId = discoveryId,
            role = ChatRole.ASSISTANT,
            content = "Answer about $discoveryId",
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        threads.value = threads.value + (discoveryId to (threads.value[discoveryId].orEmpty() + answer))
        return AppResult.Success(answer)
    }

    override suspend fun clearThread(discoveryId: String): AppResult<Unit> {
        clearedThreads += discoveryId
        threads.value = threads.value - discoveryId
        return AppResult.Success(Unit)
    }
}

/** Records what was spoken, so a test can assert the guide's reply was read aloud — or was not. */
class FakeTextToSpeech : TextToSpeechManager {
    override val isSpeaking = MutableStateFlow(false)
    override val currentUtteranceId = MutableStateFlow<String?>(null)

    val spoken = mutableListOf<String>()
    var stopCalls = 0

    override fun initialise() = Unit

    override fun speak(text: String, language: AppLanguage, utteranceId: String) {
        spoken += text
        isSpeaking.value = true
        currentUtteranceId.value = utteranceId
    }

    override fun speak(text: String, bcp47: String, utteranceId: String) {
        speak(text, AppLanguage.ENGLISH, utteranceId)
    }

    override fun stop() {
        stopCalls++
        isSpeaking.value = false
        currentUtteranceId.value = null
    }

    override fun shutdown() = stop()
}

class FakeProvinceRepository : ProvinceRepository {
    val passport = MutableStateFlow(TravelPassport(stamps = emptyList()))

    /**
     * The backfill runs in `init` and is the one call on this screen with no button behind
     * it, so a throw from it has no user action to be reported against — which is exactly
     * why it needs a switch of its own.
     */
    var throwOnBackfill: Throwable? = null
    var backfillCalls = 0

    override suspend fun provinces(): List<Province> = emptyList()
    override suspend fun provinceAt(location: GeoPoint): Province? = null
    override fun observePassport(): Flow<TravelPassport> = passport
    override suspend fun backfillProvinces(): Int {
        backfillCalls++
        throwOnBackfill?.let { throw it }
        return 0
    }
}

class FakeCatalogRepository : CatalogRepository {
    val collection = MutableStateFlow(CultureCollection(sections = emptyList()))

    override fun observeCollection(): Flow<CultureCollection> = collection
}

/** The canonical nearby place, so a suite can name one without spelling out fifteen fields. */
fun nearbyPlace(
    id: String = "p1",
    name: String = "Hoan Kiem Lake",
    location: GeoPoint = GeoPoint(21.0287, 105.8524),
): NearbyPlace = NearbyPlace(
    id = id,
    name = name,
    localName = null,
    category = DiscoveryCategory.LANDMARK,
    typeLabel = null,
    location = location,
    summary = null,
    photoUrl = null,
    distanceMeters = 400,
    monthlyReaders = null,
    mappingDetail = 1,
    openingHours = null,
    website = null,
    phone = null,
    cuisine = null,
    wikipediaTitle = null,
)

/**
 * The open map of the world, with both ways it can go wrong.
 *
 * `failOn…` is the handled path the repository promises; `throwOn…` is the crash floor —
 * the read that was never wrapped. Explore keeps a spinner flag up across both calls, so
 * which of the two happens decides whether the screen ever comes back.
 */
class FakePlaceRepository : PlaceRepository {

    var places: AppResult<List<NearbyPlace>> = AppResult.Success(emptyList())
    var details: AppResult<PlaceDetails> = AppResult.Success(PlaceDetails.empty("p1"))

    var throwOnNearby: Throwable? = null
    var throwOnDetails: Throwable? = null

    /** Virtual-clock delays, so a test can leave a request in flight and start another. */
    var nearbyDelayMillis: Long = 0
    var detailsDelayMillis: Long = 0

    var nearbyCalls = 0
    val detailsAskedFor = mutableListOf<String>()

    override suspend fun nearbyPlaces(
        center: GeoPoint,
        radiusMeters: Int,
        language: AppLanguage,
        forceRefresh: Boolean,
    ): AppResult<List<NearbyPlace>> {
        nearbyCalls++
        throwOnNearby?.let { throw it }
        if (nearbyDelayMillis > 0) delay(nearbyDelayMillis)
        return places
    }

    override suspend fun placeDetails(
        place: NearbyPlace,
        language: AppLanguage,
    ): AppResult<PlaceDetails> {
        detailsAskedFor += place.id
        throwOnDetails?.let { throw it }
        if (detailsDelayMillis > 0) delay(detailsDelayMillis)
        return details
    }
}

/**
 * The traveller's own writing.
 *
 * [throwOnSave] is the one that matters most in this file: a note is the only thing in the
 * app the traveller typed themselves, and the screen raises a saving flag before the call
 * that both disables the button and guards re-entry.
 */
class FakeNoteRepository : NoteRepository {

    val notes = MutableStateFlow<Map<String, DiscoveryNote>>(emptyMap())

    var throwOnSave: Throwable? = null
    var throwOnDelete: Throwable? = null
    var failOnSave: AppError? = null
    var failOnDelete: AppError? = null
    val saved = mutableListOf<Pair<String, String>>()

    override fun observeNote(discoveryId: String): Flow<DiscoveryNote?> =
        notes.map { it[discoveryId] }

    override suspend fun save(
        discoveryId: String,
        body: String,
        photoPaths: List<String>,
    ): AppResult<Unit> {
        saved += discoveryId to body
        throwOnSave?.let { throw it }
        failOnSave?.let { return AppResult.Failure(it) }
        notes.value = notes.value + (
            discoveryId to DiscoveryNote(
                discoveryId = discoveryId,
                body = body,
                photoPaths = photoPaths,
                createdAt = Instant.fromEpochSeconds(1_700_000_000),
                updatedAt = Instant.fromEpochSeconds(1_700_000_000),
            )
            )
        return AppResult.Success(Unit)
    }

    override suspend fun delete(discoveryId: String): AppResult<Unit> {
        throwOnDelete?.let { throw it }
        failOnDelete?.let { return AppResult.Failure(it) }
        notes.value = notes.value - discoveryId
        return AppResult.Success(Unit)
    }
}

/**
 * What the traveller has objected to.
 *
 * Carries both a `throwOn` and a `failOn` hook for its one write, for the reason
 * `LLM.md` §11 row #26 gives: no `throwOn…` can reach the branch a *handled* failure takes,
 * and it was that branch — the ordinary one — that silently succeeded on four writes of this
 * screen. `SubmitReport` raises a flag its own guard reads, so both paths have to be driven.
 */
class FakeReportRepository : ReportRepository {

    val reports = MutableStateFlow<Map<String, DiscoveryReport>>(emptyMap())

    var throwOnSubmit: Throwable? = null
    var failOnSubmit: AppError? = null

    /** Every call, in order, so a test can prove a retry really reached the repository. */
    val submitted = mutableListOf<Pair<ReportReason, String>>()

    override fun observeReport(discoveryId: String): Flow<DiscoveryReport?> =
        reports.map { it[discoveryId] }

    override suspend fun submit(
        discoveryId: String,
        reason: ReportReason,
        note: String,
    ): AppResult<DiscoveryReport> {
        // Recorded before either hook fires: a test that counts retries has to see the
        // attempt that failed as well as the one that worked.
        submitted += reason to note
        throwOnSubmit?.let { throw it }
        failOnSubmit?.let { return AppResult.Failure(it) }
        val filed = DiscoveryReport(
            discoveryId = discoveryId,
            reason = reason,
            note = note,
            createdAt = Instant.fromEpochSeconds(1_700_000_000),
        )
        reports.value = reports.value + (discoveryId to filed)
        return AppResult.Success(filed)
    }
}

/** The canonical translation, with one block so `translatedText` is non-empty. */
fun translationResult(id: String = "t1"): TranslationResult = TranslationResult(
    id = id,
    imagePath = "/captures/capture_1.jpg",
    detectedLanguage = "vi",
    targetLanguage = "en",
    blocks = listOf(
        TranslationBlock(original = "Phở bò", translated = "Beef noodle soup", note = null, price = null),
    ),
    contextNote = null,
    createdAt = Instant.fromEpochSeconds(1_700_000_000),
)

class FakeTranslationRepository : TranslationRepository {

    val translations = MutableStateFlow<List<TranslationResult>>(emptyList())

    var result: AppResult<TranslationResult> = AppResult.Success(translationResult())
    var throwOnTranslate: Throwable? = null

    /** Long enough for a test to prove the screen's own timeout fires before the call returns. */
    var translateDelayMillis: Long = 0
    var translateCalls = 0

    override suspend fun translate(
        imagePath: String,
        sourceLanguage: TranslateLanguage?,
        targetLanguage: TranslateLanguage,
    ): AppResult<TranslationResult> {
        translateCalls++
        throwOnTranslate?.let { throw it }
        if (translateDelayMillis > 0) delay(translateDelayMillis)
        return result
    }

    override fun observeTranslations(): Flow<List<TranslationResult>> = translations
    override fun observeTranslation(id: String): Flow<TranslationResult?> =
        translations.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun delete(id: String): AppResult<Unit> = AppResult.Success(Unit)
}

/** The launch-time orphan sweep, which nobody is waiting on and nothing reports. */
class FakeCaptureMaintenance : CaptureMaintenance {
    var throwOnSweep: Throwable? = null
    var sweepCalls = 0

    override suspend fun sweepOrphans(): Int {
        sweepCalls++
        throwOnSweep?.let { throw it }
        return 0
    }
}

/**
 * Records that the window host asked for a seed, and can refuse.
 *
 * `throwOnSeed` exists because the real implementation promises never to throw and the window
 * host has no `try` of its own around the call — so the only way to prove that promise is load
 * bearing is to break it here and watch the sweep behind it still run.
 */
class FakeDemoDataSeeder : DemoDataSeeder {
    var throwOnSeed: Throwable? = null
    var seedCalls = 0

    override suspend fun seedIfEmpty() {
        seedCalls++
        throwOnSeed?.let { throw it }
    }
}
