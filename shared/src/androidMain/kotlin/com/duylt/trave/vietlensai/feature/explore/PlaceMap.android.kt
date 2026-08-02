package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.duylt.trave.vietlensai.domain.model.GeoPoint
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import com.google.android.gms.maps.CameraUpdateFactory
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

/**
 * The Explore map on Android, drawn by the Maps SDK.
 *
 * The key it needs is `com.google.android.geo.API_KEY` in `:app`'s manifest. Without one
 * the SDK still lays out and still handles gestures — it simply renders no tiles, which
 * is why the screen above draws its own "map unavailable" hint rather than relying on
 * this to fail loudly.
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
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            (camera?.target ?: userLocation ?: GeoPoint.HANOI_CENTER).toLatLng(),
            (camera?.zoom ?: MapZoom.NEIGHBOURHOOD).level,
        )
    }

    // Keyed on the request id, not on the target: two taps on the same marker are two
    // requests to go there, and the second one has to move a camera the traveller has
    // panned away from since the first.
    LaunchedEffect(camera?.requestId) {
        val target = camera ?: return@LaunchedEffect
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(target.target.toLatLng(), target.zoom.level),
            durationMs = CAMERA_ANIMATION_MILLIS,
        )
        // Only after the animation has run. Retiring the request before it finishes
        // would cancel this effect halfway through the move.
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
                    state = rememberUpdatedMarkerState(place.location.toLatLng()),
                    // Carried for accessibility rather than for display: the info window
                    // is suppressed below, and the sheet is what shows this place.
                    title = place.name,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        markerColor(place.category).mapsHue(),
                    ),
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
