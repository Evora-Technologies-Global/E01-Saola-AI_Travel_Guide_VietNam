package com.evora.technologies.saola.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Forest
import androidx.compose.material.icons.outlined.Museum
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.TempleBuddhist
import com.evora.technologies.saola.core.designsystem.theme.CategoryColors
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace

/**
 * A real map with the traveller on it and a pin on everything worth walking to.
 *
 * The one `expect` in this feature, and the only thing about it that genuinely differs
 * per platform: Android draws it with the Maps SDK and iOS with MapKit, because there
 * is no Google map on iOS to draw. Everything above it — what gets searched for, how
 * the results are ranked, what a marker means, what the sheet says — is written once
 * and is identical on both.
 *
 * The contract is kept deliberately narrow, in domain types and callbacks rather than
 * in anything either SDK understands. Two implementations only stay in step for as long
 * as neither of them is given a reason to know something the other does not.
 *
 * @param places every marker to draw. Colour comes from [markerColor], so a temple is
 *   the same red here that it is in the journal.
 * @param selectedPlaceId drawn larger and in front, so the pin behind an open sheet is
 *   still findable.
 * @param userLocation only for framing. The blue dot itself is the platform's own, from
 *   its own location subscription — a dot this app placed by hand would lag behind the
 *   traveller by however long a fix takes.
 * @param camera where to move the view, or null to leave it wherever the traveller
 *   dragged it. See [MapCamera].
 * @param onCameraApplied called once the move has been carried out, so the request can
 *   be retired. Without it a camera move is a value that lives in state for ever rather
 *   than an instruction, and the map re-applies the last one every time it recomposes —
 *   snapping the traveller back to where they were the previous time they opened the tab.
 *
 * There is deliberately no `onMapClick`. The Maps SDK offers one and MapKit does not —
 * getting the same behaviour there would mean hanging a `UITapGestureRecognizer` off the
 * map view and reasoning about which taps MapKit had already claimed. It would buy
 * nothing: the only thing a tap on the map would do is close the sheet, and the sheet is
 * modal, so its own scrim already closes it on both platforms.
 */
@Composable
expect fun PlaceMap(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    userLocation: GeoPoint?,
    camera: MapCamera?,
    isDarkTheme: Boolean,
    onPlaceSelected: (String) -> Unit,
    onCameraApplied: () -> Unit,
    modifier: Modifier = Modifier,
)

/**
 * A request to move the camera, rather than a description of where it is.
 *
 * [requestId] is what makes it a request. Without it the camera is a value the map
 * merely mirrors, and the second tap on a marker the traveller has since panned away
 * from would do nothing at all — the target is unchanged, so nothing recomposes. Bumped
 * by the view model on every explicit move, it turns "here is the target" into "go
 * there now", which is the thing the screen actually means.
 */
@Immutable
data class MapCamera(
    val target: GeoPoint,
    val zoom: MapZoom,
    val requestId: Long,
)

/**
 * How much ground the map should show, named rather than numbered.
 *
 * The two SDKs do not measure this the same way — Google takes a zoom level on a
 * logarithmic scale, MapKit takes a span in metres — so a number here would have to be
 * one of theirs, and the other platform would be left converting it approximately. A
 * name is exact on both.
 */
enum class MapZoom {
    /** The whole 5 km search, so every marker is on screen at once. */
    NEIGHBOURHOOD,

    /** Close enough to see which street a place is on. */
    PLACE,
}

/**
 * The colour a category's pin is drawn in.
 *
 * The app's existing category palette rather than a set of map colours, because these
 * pins are not the traveller's first encounter with it — they have already met lacquer
 * red on temples and amber on food in the journal and on the culture board. A map with
 * its own scheme would be a second thing to learn for no gain.
 */
fun markerColor(category: DiscoveryCategory): Color = when (category) {
    DiscoveryCategory.LANDMARK -> CategoryColors.landmark
    DiscoveryCategory.FOOD -> CategoryColors.food
    DiscoveryCategory.ARTIFACT -> CategoryColors.artifact
    DiscoveryCategory.ARCHITECTURE -> CategoryColors.architecture
    DiscoveryCategory.NATURE -> CategoryColors.nature
    DiscoveryCategory.CULTURE -> CategoryColors.culture
    DiscoveryCategory.OTHER -> CategoryColors.other
}

/**
 * The glyph that stands in for a place with no photograph.
 *
 * Most places have none. OpenStreetMap holds no images, and only the well-known few
 * carry a Wikipedia article or have been photographed onto Commons near enough to
 * match — measured in a residential district of Hanoi, that was none of forty. A
 * generic "image unavailable" mark on every card reads as a screen that failed to
 * load; a temple drawn as a temple in its own category colour reads as a design.
 */
fun categoryIcon(category: DiscoveryCategory): ImageVector = when (category) {
    DiscoveryCategory.LANDMARK -> Icons.Outlined.Place
    DiscoveryCategory.FOOD -> Icons.Outlined.Restaurant
    DiscoveryCategory.ARTIFACT -> Icons.Outlined.Brush
    DiscoveryCategory.ARCHITECTURE -> Icons.Outlined.TempleBuddhist
    DiscoveryCategory.NATURE -> Icons.Outlined.Forest
    DiscoveryCategory.CULTURE -> Icons.Outlined.Museum
    DiscoveryCategory.OTHER -> Icons.Outlined.AccountBalance
}
