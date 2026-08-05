package com.evora.technologies.saola.feature.explore

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.evora.technologies.saola.core.util.log
import com.evora.technologies.saola.domain.model.DiscoveryCategory
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Explore map on Android, drawn by the Maps SDK.
 *
 * The key it needs is `com.google.android.geo.API_KEY` in `:app`'s manifest. Without one
 * the SDK still lays out and still handles gestures — it simply renders no tiles, which
 * is why the screen above draws its own "map unavailable" hint rather than relying on
 * this to fail loudly.
 *
 * Nothing here composes until [loadMapEngine] has run. That is not a style choice; see
 * its KDoc for the second of frames it is worth on the first open of the tab.
 */
@Composable
actual fun PlaceMap(
    places: List<NearbyPlace>,
    selectedPlaceId: String?,
    userLocation: GeoPoint?,
    camera: MapCamera?,
    isDarkTheme: Boolean,
    onPlaceSelected: (String) -> Unit,
    onCameraApplied: () -> Unit,
    modifier: Modifier,
) {
    val context = LocalContext.current
    // Seeded from the process-wide flag, so the second visit to this tab starts ready and
    // never draws the placeholder at all.
    var isEngineReady by remember { mutableStateOf(isMapEngineLoaded) }

    LaunchedEffect(Unit) {
        if (!isEngineReady) {
            withContext(Dispatchers.IO) { loadMapEngine(context.applicationContext) }
            isEngineReady = true
        }
    }

    if (!isEngineReady) {
        // The ground the map will stand on, so the hand-over is a picture appearing on a
        // surface rather than a surface appearing under a picture. In practice this is
        // never seen: `ExploreScreen` composes this while its loading cover is still up.
        Box(modifier = modifier.background(MaterialTheme.colorScheme.background))
        return
    }

    // Seven descriptors, built once — not one per marker on every recomposition, which is
    // what `BitmapDescriptorFactory.defaultMarker(...)` written at the call site meant. A
    // descriptor is a handle into the renderer and two of them are never `equals`, so the
    // marker node dutifully pushed a "new" icon across the SDK boundary for every pin on
    // the map every time anything on this screen changed — opening one sheet is three
    // emissions, which was three sweeps of the whole map. `mapsHue` is not free either:
    // it allocates and calls into `android.graphics`.
    val markerIcons = remember { categoryMarkerIcons() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            (camera?.target ?: userLocation ?: GeoPoint.HANOI_CENTER).toLatLng(),
            (camera?.zoom ?: MapZoom.NEIGHBOURHOOD).level,
        )
    }

    // Whether this map opened on the Hanoi fallback because neither a request nor a fix
    // existed to open it on — see the branch below. The condition is read once, at first
    // composition, which is the only moment it describes.
    var isAwaitingFirstFix by remember { mutableStateOf(camera == null && userLocation == null) }

    // Keyed on the request id, not on the target: two taps on the same marker are two
    // requests to go there, and the second one has to move a camera the traveller has
    // panned away from since the first.
    LaunchedEffect(camera?.requestId) {
        val target = camera ?: return@LaunchedEffect
        val position = CameraPosition.fromLatLngZoom(target.target.toLatLng(), target.zoom.level)

        if (isAwaitingFirstFix) {
            // Set, not flown. This map is composed before the first fix arrives, so it
            // opened on the fallback and this request is the correction — animating it
            // would fly the traveller in from Hanoi over a city they may be nowhere near.
            // Assigning `position` is also the only form that is safe this early: it
            // applies whether or not the map view has finished attaching itself, which
            // `animate` cannot promise.
            //
            // Only the fallback case. Coming back to the tab, the camera state is seeded
            // from the fix already in state, so there is nothing to correct and every
            // move from then on is one the traveller asked for.
            isAwaitingFirstFix = false
            cameraPositionState.position = position
        } else {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newCameraPosition(position),
                durationMs = CAMERA_ANIMATION_MILLIS,
            )
        }
        // Only after the move has run. Retiring the request before it finishes would
        // cancel this effect halfway through.
        onCameraApplied()
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = remember(userLocation != null) {
            MapProperties(
                mapType = MapType.NORMAL,
                // The platform's own blue dot, from the SDK's own location subscription.
                // Enabled only once a fix has arrived, which is the app's proxy for "the
                // permission was granted" — the SDK throws a SecurityException if it is
                // switched on without one.
                isMyLocationEnabled = userLocation != null,
            )
        },
        uiSettings = remember {
            MapUiSettings(
                // The screen has a recentre control of its own, placed where the rest of
                // the app puts its floating controls. The SDK's would sit on top of it.
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                mapToolbarEnabled = false,
                // Left on: a two-finger twist is how people look along a street, and a
                // map that refuses it feels like a picture of a map.
                rotationGesturesEnabled = true,
                tiltGesturesEnabled = false,
            )
        },
        // Google's own night styling rather than a hand-written JSON style: it is
        // designed against the same tiles and stays right when they change.
        mapColorScheme = if (isDarkTheme) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT,
    ) {
        places.forEach { place ->
            // `key` rather than relying on position in the list: the list is re-sorted on
            // every refresh, and without an identity the marker composables would be
            // re-used against different places and animate across the map.
            key(place.id) {
                val isSelected = place.id == selectedPlaceId
                Marker(
                    // Remembered on the point rather than rebuilt: a `LatLng` allocated
                    // fresh each pass is a new object the marker state has to compare
                    // against, for a place that has not moved since it was mapped.
                    state = rememberUpdatedMarkerState(
                        remember(place.location) { place.location.toLatLng() },
                    ),
                    // Carried for accessibility rather than for display: the info window
                    // is suppressed below, and the sheet is what shows this place.
                    title = place.name,
                    icon = markerIcons[place.category],
                    // Dimmed rather than hidden while another place is open: the shape of
                    // the cluster is what tells the traveller whether they are in a busy
                    // quarter or a quiet one, and it should survive opening one sheet.
                    alpha = if (selectedPlaceId == null || isSelected) 1f else UNSELECTED_ALPHA,
                    zIndex = if (isSelected) 1f else 0f,
                    onClick = {
                        onPlaceSelected(place.id)
                        // True: consume it. The default would centre the camera and open
                        // an info window, and the sheet is what shows this place now.
                        true
                    },
                )
            }
        }
    }
}

/**
 * Whether this process has already paid for the Maps renderer.
 *
 * Process-wide rather than per-composition, and `@Volatile` because it is written on a
 * background thread and read on the main one. The SDK loads its renderer once and keeps
 * it, so the second visit to this tab must not wait on a question already answered.
 */
@Volatile
private var isMapEngineLoaded = false

/**
 * Fetches and links the Maps renderer, off the main thread.
 *
 * **This is the fix for the stutter on the first open of the Explore tab.** The Maps SDK
 * keeps its renderer in Google Play services and loads it the first time anything asks
 * for a map: a module resolution, a large amount of class loading, and the graphics
 * pipeline behind it. That work is synchronous and it happens on whatever thread asked —
 * and inside `GoogleMap` the thread that asks is the main one, so the app freezes for as
 * long as it takes. On a mid-range phone that is most of a second, once per process,
 * landing on the exact frame the traveller opened the tab.
 *
 * `MapsInitializer.initialize` is `static synchronized`, which is what makes moving it
 * here safe: it is the identical work, done on a thread where stopping does not show,
 * and `GoogleMap` finds it already done. `CameraUpdateFactory` and
 * `BitmapDescriptorFactory` — both used above — are also only legal once it has run.
 *
 * The flag is set whatever the outcome. A device with no Play services cannot be given a
 * renderer at all, and this screen already draws a map that renders no tiles rather than
 * treating that as a failure to report; blocking on a retry loop would turn a degraded
 * map into no screen.
 */
private fun loadMapEngine(context: Context) {
    runCatching { MapsInitializer.initialize(context) }
        .onFailure { log.w(it) { "Could not preload the Maps renderer" } }
    isMapEngineLoaded = true
}

/** One pin per category, in the app's own palette. Only legal after [loadMapEngine]. */
private fun categoryMarkerIcons(): Map<DiscoveryCategory, BitmapDescriptor> =
    DiscoveryCategory.entries.associateWith { category ->
        BitmapDescriptorFactory.defaultMarker(markerColor(category).mapsHue())
    }

private fun GeoPoint.toLatLng() = LatLng(latitude, longitude)

/**
 * Google's zoom levels for the two amounts of ground the shared code asks for.
 *
 * Chosen to match the metre spans MapKit is given on the other side: at Vietnam's
 * latitude zoom 13 puts roughly 7 km across a phone display and zoom 16 roughly 900 m,
 * so the two platforms open on the same view rather than merely a similar one.
 *
 * Note that 13 shows less than the full 5 km radius, on purpose. Framing the entire
 * search circle would fit every marker on screen at the cost of making them a cluster of
 * dots — and almost everything worth walking to is in the first kilometre anyway.
 */
private val MapZoom.level: Float
    get() = when (this) {
        MapZoom.NEIGHBOURHOOD -> 13f
        MapZoom.PLACE -> 16f
    }

/**
 * The hue of a colour, on the 0–360 scale `BitmapDescriptorFactory` wants.
 *
 * The stock pin tinted by hue rather than a custom bitmap: it is the pin every Android
 * user already knows how to read, it stays crisp at every density without shipping an
 * asset, and it costs nothing to draw twenty-four of. Saturation and lightness are
 * discarded by the factory, so the pins are more vivid than the chips they match — but
 * the hue is the part the eye uses to pair them, and it survives exactly.
 */
private fun Color.mapsHue(): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    return hsv[0]
}

private const val UNSELECTED_ALPHA = 0.55f
private const val CAMERA_ANIMATION_MILLIS = 600
