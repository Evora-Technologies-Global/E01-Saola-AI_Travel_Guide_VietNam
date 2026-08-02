package com.duylt.trave.vietlensai.feature.sovereignty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.util.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import com.duylt.trave.vietlensai.resources.Res

/**
 * Holds the locator map the statement is built around.
 *
 * The asset comes from Compose Resources rather than from an `assets/` folder: the file is a
 * UI figure, it is only ever read by this screen, and `Res.readBytes` packages and reads it
 * identically on both platforms — no asset manager on one side and no bundle lookup on the
 * other.
 */
class SovereigntyViewModel : ViewModel() {

    private val _map = MutableStateFlow<RegionMap?>(null)

    /** Null until the asset has been read; the page draws its prose meanwhile. */
    val map: StateFlow<RegionMap?> = _map.asStateFlow()

    init {
        viewModelScope.launch { _map.value = load() }
    }

    /**
     * Reads and parses the figure off the main thread.
     *
     * `viewModelScope` is `Dispatchers.Main.immediate`, so without the [withContext] the whole
     * read-and-parse of a 102 KB JSON ran on the frame loop — and it ran during the screen's
     * own entry transition, which showed as a stutter rather than as a load. The project's own
     * asset loaders, `ProvinceAssetSource.load` and `CatalogAssetSource.load`, already do this
     * work off-thread; this screen was the one place paying for it. `Dispatchers.Default` is
     * the right pool because the cost is parsing, not waiting on disk, and it is available in
     * commonMain on both targets.
     */
    @OptIn(ExperimentalResourceApi::class)
    private suspend fun load(): RegionMap? = withContext(Dispatchers.Default) {
        try {
            parseRegionMap(Res.readBytes(ASSET_PATH).decodeToString())
        } catch (cancellation: CancellationException) {
            // Backing out of the page mid-read is an ordinary navigation, not a failure.
            // Caught by the branch below it would log an ERROR pointing at asset packaging —
            // the hardest class of problem to diagnose here — for something nobody did wrong.
            throw cancellation
        } catch (e: Exception) {
            // The words are the point of this screen and they are in the string table.
            // A map that will not parse costs the figure, not the statement.
            log.e(e) { "Could not read $ASSET_PATH" }
            null
        }
    }

    private companion object {
        const val ASSET_PATH = "files/sovereignty_map.json"
    }
}
