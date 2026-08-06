package com.evora.technologies.saola.feature.passport



import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.lifecycle.viewModelScope
import com.evora.technologies.saola.core.mvi.MviViewModel
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.model.TravelPassport
import com.evora.technologies.saola.domain.usecase.BackfillProvincesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveJournalStatsUseCase
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.usecase.ObserveProvinceDiscoveriesUseCase
import com.evora.technologies.saola.domain.usecase.ObserveSettingsUseCase
import com.evora.technologies.saola.domain.usecase.ObserveTravelPassportUseCase
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Owns the passport map: which provinces are stamped, and the photo inside each.
 *
 * Cover photos are decoded here rather than through Coil because the map does not
 * render them as images — it clips them into arbitrary polygon outlines on a
 * `Canvas`, which needs an `ImageBitmap` in hand rather than a composable that
 * paints itself into a rectangle.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PassportViewModel(
    observePassport: ObserveTravelPassportUseCase,
    observeJournalStats: ObserveJournalStatsUseCase,
    observeSettings: ObserveSettingsUseCase,
    observeProvinceDiscoveries: ObserveProvinceDiscoveriesUseCase,
    private val backfillProvinces: BackfillProvincesUseCase,
    private val captureStore: CaptureStore,
) : MviViewModel<PassportState, PassportIntent, PassportEffect>(PassportState()) {

    /** Serialises cover refreshes so two passport emissions cannot decode the same photo twice. */
    private val coverLock = Mutex()

    init {
        // Discoveries captured before the passport shipped already know where they
        // were taken. Stamping them here means a traveller who has been using the
        // app opens the map to their real journey rather than to 34 empty outlines.
        launchSafely {
            // Not `runCatching`: it catches Throwable, so leaving the passport tab while the
            // backfill is still walking unstamped rows — an ordinary action, and the window is
            // one database write per row — was reported as "Province backfill failed" at ERROR
            // every time. Cancellation is how this coroutine is told to stop, so it goes back
            // up, exactly as StorageGuards and launchSafely do one layer down.
            try {
                val stamped = backfillProvinces()
                if (stamped > 0) log.i { "Stamped $stamped earlier discoveries" }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                log.e(e) { "Province backfill failed" }
            }
        }

        observePassport()
            .onEach { passport ->
                setState { copy(isLoading = false, passport = passport) }
                refreshCovers(passport)
            }
            .launchIn(viewModelScope)

        // Tells an empty map whose fault it is: no captures yet, or captures that
        // never carried a location.
        observeJournalStats()
            .onEach { stats -> setState { copy(totalDiscoveries = stats.totalDiscoveries) } }
            .launchIn(viewModelScope)

        // Dates on the province sheet are written in whichever language the traveller
        // reads the rest of the app in.
        observeSettings()
            .onEach { settings -> setState { copy(language = settings.language) } }
            .launchIn(viewModelScope)

        // Driven off the selection in state rather than off the intent, so the
        // previous province's query is cancelled by `flatMapLatest` the moment
        // another one is tapped. Tapping across the map at speed therefore leaves at
        // most one live query behind it instead of one per province touched.
        state
            .map { it.selectedProvinceId }
            .distinctUntilChanged()
            .flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else observeProvinceDiscoveries(id)
            }
            .onEach { discoveries -> setState { copy(selectedDiscoveries = discoveries) } }
            .launchIn(viewModelScope)
    }

    override fun onIntent(intent: PassportIntent) {
        when (intent) {
            // The list is dropped with the selection, not left to be overwritten when
            // the new query lands: the sheet would otherwise spend a frame or two
            // showing the previous province's photographs under the new province's
            // name, which reads as the wrong photos rather than as a slow load.
            is PassportIntent.SelectProvince -> setState {
                if (intent.provinceId == selectedProvinceId) this
                else copy(selectedProvinceId = intent.provinceId, selectedDiscoveries = emptyList())
            }

            PassportIntent.DismissSelection ->
                setState { copy(selectedProvinceId = null, selectedDiscoveries = emptyList()) }
        }
    }

    /**
     * Decodes covers that are new or whose photo has changed, and drops those whose
     * discovery is gone.
     *
     * Diffed on the *path*, not just the province id. Keying on the id alone would
     * mean a province that already had a cover kept showing it after the traveller
     * deleted that photo or favourited a different one — the row changes, the id
     * does not, and the stale bitmap would survive until the process died.
     */
    private fun refreshCovers(passport: TravelPassport) {
        val wanted = passport.stamps
            .mapNotNull { stamp -> stamp.coverImagePath?.let { stamp.province.id to it } }
            .toMap()

        launchSafely {
            coverLock.withLock {
                val held = currentState.covers
                val toLoad = wanted.filter { (id, path) -> held[id]?.path != path }
                val keep = held.filterKeys { wanted[it] != null }
                if (toLoad.isEmpty() && keep.size == held.size) return@withLock

                val loaded = HashMap<String, CoverPhoto>(toLoad.size)
                toLoad.forEach { (provinceId, path) ->
                    // Leaving the tab should not keep the whole batch decoding.
                    coroutineContext.ensureActive()
                    decodeCover(path)?.let { loaded[provinceId] = CoverPhoto(path, it) }
                }
                setState { copy(covers = keep + loaded) }
            }
        }
    }

    /**
     * Reads a capture, upright, small enough for a province outline.
     *
     * Goes through [CaptureStore] rather than decoding the file here. That is what makes this
     * screen multiplatform, but it also fixes something the Android-only version got wrong: the
     * store applies the EXIF rotation, while the raw `BitmapFactory` decode this replaced did
     * not — so a portrait capture used to lie on its side inside the province while the same
     * photo stood upright one tap away in the detail sheet.
     *
     * Sampling is the store's business too, and it is not optional: a province occupies 40–80 px
     * at country zoom, so decoding a 12-megapixel JPEG for one would spend ~48 MB to fill a
     * thumbnail, and 34 of those would be a certain OOM.
     */
    private suspend fun decodeCover(path: String): ImageBitmap? =
        when (val read = captureStore.read(path)) {
            is AppResult.Failure -> {
                log.w { "Could not read cover $path: ${read.error}" }
                null
            }
            // Off the main thread: JPEG decode and the resample below are CPU-bound, and
            // this runs 34 times in a row on a screen the traveller is already looking at.
            // `viewModelScope` is `Dispatchers.Main.immediate`, so without this the whole
            // batch blocked the frame loop.
            is AppResult.Success -> withContext(Dispatchers.Default) {
                runCatching { read.data.bytes.decodeToImageBitmap().scaledToCover() }
                    .onFailure { log.w(it) { "Could not decode cover $path" } }
                    .getOrNull()
            }
        }

    /**
     * Resamples a capture down to [COVER_SIZE_PX] on its longest edge.
     *
     * [CaptureStore] hands back the upload size — 1024 px, chosen for what the recogniser
     * needs — and this screen holds one of those per stamped province, all at once, in a
     * `StateFlow` that lives as long as the tab. Thirty-four of them is about 107 MB of
     * `ImageBitmap`, which is not a leak but is simply more than a mid-range phone has;
     * [COVER_SIZE_PX] brings the same set to under 7 MB.
     *
     * `COVER_SIZE_PX` was already declared, with a KDoc above `decodeCover` explaining that
     * "a province occupies 40–80 px at country zoom" and that not sampling would be "a certain
     * OOM". Nothing ever read the constant. That is the whole defect: the reasoning was right
     * and was never connected to the code.
     *
     * Done here in Compose rather than by adding a thumbnail port to [CaptureStore], because
     * `ImageBitmap`, `Canvas` and `drawImageRect` are all multiplatform — so one implementation
     * covers both phones, and the full-size decode is transient rather than retained.
     */
    private fun ImageBitmap.scaledToCover(): ImageBitmap {
        val longestEdge = maxOf(width, height)
        if (longestEdge <= COVER_SIZE_PX) return this

        val targetWidth = (width.toLong() * COVER_SIZE_PX / longestEdge).toInt().coerceAtLeast(1)
        val targetHeight = (height.toLong() * COVER_SIZE_PX / longestEdge).toInt().coerceAtLeast(1)

        val target = ImageBitmap(targetWidth, targetHeight)
        Canvas(target).drawImageRect(
            image = this,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(width, height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(targetWidth, targetHeight),
            // Filtered: the cover is clipped into a province outline, so nearest-neighbour
            // aliasing shows up as jagged edges along the coastline.
            paint = Paint().apply { filterQuality = FilterQuality.Medium },
        )
        return target
    }

    private companion object {
        /**
         * The longest edge a cover is kept at.
         *
         * A province occupies 40–80 px at country zoom and up to roughly 250 px zoomed in on
         * the largest one, so 256 is the point past which nothing on screen improves.
         */
        const val COVER_SIZE_PX = 256
    }
}
