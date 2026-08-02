package com.duylt.trave.vietlensai.testing

import com.duylt.trave.vietlensai.domain.model.AppLanguage
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.domain.model.CaptureImage
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.domain.model.DiscoveryCategory
import com.duylt.trave.vietlensai.domain.model.GeminiModel
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.LensMode
import com.duylt.trave.vietlensai.domain.model.ThemePreference
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.DiscoveryRepository
import com.duylt.trave.vietlensai.domain.repository.LocationRepository
import com.duylt.trave.vietlensai.domain.repository.SettingsRepository
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
    modelUsed = GeminiModel.DEFAULT.id,
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

    override fun observeByProvince(provinceId: String): Flow<List<Discovery>> = discoveries
    override suspend fun getDiscovery(id: String): Discovery? =
        discoveries.value.firstOrNull { it.id == id }

    override suspend fun toggleFavorite(id: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun delete(id: String): AppResult<Unit> = AppResult.Success(Unit)
    override suspend fun deleteAll(): AppResult<Unit> = AppResult.Success(Unit)
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

    override suspend fun setApiKey(key: String?) {
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(apiKey = key)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(language = language)
    }

    override suspend fun setModel(model: GeminiModel) {
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(preferredModel = model)
    }

    override suspend fun setSpeakAnswers(enabled: Boolean) {
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(speakAnswers = enabled)
    }

    override suspend fun setLocationAsked() {
        locationAskedCalls++
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(hasAskedLocation = true)
    }

    override suspend fun setThemePreference(preference: ThemePreference) {
        throwOnWrite?.let { throw it }
        state.value = state.value.copy(darkTheme = preference)
    }
}

class FakeCaptureStore : CaptureStore {
    var nextPath = "/captures/capture_1.jpg"
    var throwOnRead: Throwable? = null
    val deleted = mutableListOf<String?>()

    override fun newCapturePath(): String = nextPath

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
