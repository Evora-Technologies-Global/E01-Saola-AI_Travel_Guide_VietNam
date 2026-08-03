package com.duylt.trave.vietlensai.feature.sovereignty

import com.duylt.trave.vietlensai.core.mvi.MviViewModel
import com.duylt.trave.vietlensai.core.util.log
import com.duylt.trave.vietlensai.resources.Res
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Holds the locator map the statement is built around.
 *
 * The asset comes from Compose Resources rather than from an `assets/` folder: the file is a
 * UI figure, it is only ever read by this screen, and `Res.readBytes` packages and reads it
 * identically on both platforms — no asset manager on one side and no bundle lookup on the
 * other.
 *
 * On [MviViewModel] like every other screen even though nothing is ever sent to it and nothing
 * is ever emitted from it. The value is not the empty channel — it is that ten feature packages
 * present ten identical shapes, so a reader arriving at this one has no exception to reason
 * about. `CollectionEffect` already sets that precedent for an empty effect set.
 */
class SovereigntyViewModel :
    MviViewModel<SovereigntyState, SovereigntyIntent, SovereigntyEffect>(SovereigntyState()) {

    init {
        // Through `launchSafely` rather than a bare `viewModelScope.launch`, so the floor under
        // it is the same one every other screen has: `load` answers for the parse itself, and
        // this catches whatever it did not — a resource the platform could not open at all.
        launchSafely(onError = { setState { copy(isLoading = false) } }) {
            val parsed = load()
            setState { copy(isLoading = false, map = parsed) }
        }
    }

    /** Read-only screen: there is nothing the traveller can send. */
    override fun onIntent(intent: SovereigntyIntent) = Unit

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
            // A map that will not parse costs the figure, not the statement. Logged here
            // rather than left to `launchSafely`, which would name the ViewModel instead of
            // the asset — and the asset is the only useful thing to know.
            log.e(e) { "Could not read $ASSET_PATH" }
            null
        }
    }

    private companion object {
        const val ASSET_PATH = "files/sovereignty_map.json"
    }
}
