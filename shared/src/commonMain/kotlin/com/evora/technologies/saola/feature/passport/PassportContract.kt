package com.evora.technologies.saola.feature.passport

import androidx.compose.ui.graphics.ImageBitmap
import com.evora.technologies.saola.core.mvi.UiEffect
import com.evora.technologies.saola.core.mvi.UiIntent
import com.evora.technologies.saola.core.mvi.UiState
import com.evora.technologies.saola.domain.model.AppLanguage
import com.evora.technologies.saola.domain.model.Discovery
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.domain.model.TravelPassport
import kotlin.time.Instant

/**
 * A decoded cover, kept next to the path it came from.
 *
 * The path is what makes the cache correct: it is how the view model notices that a
 * province's photo has been replaced or deleted, which a province id alone cannot
 * tell it.
 */
data class CoverPhoto(
    val path: String,
    val image: ImageBitmap,
)

data class PassportState(
    val isLoading: Boolean = true,
    val passport: TravelPassport = TravelPassport.EMPTY,
    /** Province id → decoded cover, kept small enough to draw 34 of at once. */
    val covers: Map<String, CoverPhoto> = emptyMap(),
    /**
     * Every discovery in the journal, stamped or not.
     *
     * Counted separately from the passport because the passport only knows about
     * discoveries that resolved to a province — the number it cannot see is exactly
     * the one needed to tell "has not travelled yet" apart from "location is off".
     */
    val totalDiscoveries: Int = 0,
    val selectedProvinceId: String? = null,
    /**
     * What was captured in the selected province, newest first.
     *
     * Loaded only while a province is open — 34 provinces' worth of rows would be the
     * whole journal held in memory for a sheet that shows one of them at a time.
     */
    val selectedDiscoveries: List<Discovery> = emptyList(),
    val language: AppLanguage = AppLanguage.VIETNAMESE,
) : UiState {

    val selected: PassportStamp?
        get() = selectedProvinceId?.let { id -> passport.stamps.firstOrNull { it.province.id == id } }

    /**
     * Where the selected province falls in the order they were unlocked — "the 3rd
     * province you opened" — or null if it is still locked.
     *
     * Ranked by first visit, which is the only ordering a collection has. Ties are
     * impossible in practice and harmless if they happen: two provinces stamped in
     * the same millisecond would simply share a neighbouring pair of numbers.
     */
    val selectedUnlockOrder: Int?
        get() {
            val stamp = selected?.takeIf { it.isUnlocked } ?: return null
            val visitedAt = stamp.firstVisitAt ?: return null
            return passport.stamps
                .count { it.isUnlocked && (it.firstVisitAt ?: Instant.DISTANT_FUTURE) <= visitedAt }
        }

    /**
     * The traveller has captured things, but not one of them knows where it was
     * taken — the signature of location being switched off.
     *
     * Worth distinguishing: telling someone with twenty discoveries to go and
     * discover something would be both wrong and slightly insulting, when the real
     * problem is a permission they can grant in one tap.
     */
    val hasDiscoveriesButNoLocation: Boolean
        get() = !isLoading && passport.unlockedCount == 0 && totalDiscoveries > 0
}

sealed interface PassportIntent : UiIntent {
    data class SelectProvince(val provinceId: String?) : PassportIntent
    data object DismissSelection : PassportIntent
}

sealed interface PassportEffect : UiEffect
