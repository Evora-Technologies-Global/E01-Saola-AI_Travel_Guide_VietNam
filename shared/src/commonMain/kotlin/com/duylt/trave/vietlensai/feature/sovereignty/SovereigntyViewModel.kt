package com.duylt.trave.vietlensai.feature.sovereignty

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duylt.trave.vietlensai.core.util.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun load(): RegionMap? = try {
        parseRegionMap(Res.readBytes(ASSET_PATH).decodeToString())
    } catch (e: Exception) {
        // The words are the point of this screen and they are in the string table.
        // A map that will not parse costs the figure, not the statement.
        log.e(e) { "Could not read $ASSET_PATH" }
        null
    }

    private companion object {
        const val ASSET_PATH = "files/sovereignty_map.json"
    }
}
