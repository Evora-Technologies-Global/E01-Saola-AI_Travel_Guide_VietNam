package com.evora.technologies.saola.feature.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import com.evora.technologies.saola.domain.model.GeoPoint
import com.evora.technologies.saola.domain.model.NearbyPlace
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2D
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKAnnotationProtocol
import platform.MapKit.MKAnnotationView
import platform.MapKit.MKCoordinateRegion
import platform.MapKit.MKCoordinateRegionMakeWithDistance
import platform.MapKit.MKFeatureDisplayPriorityDefaultHigh
import platform.MapKit.MKFeatureDisplayPriorityRequired
import platform.MapKit.MKMapView
import platform.MapKit.MKMapViewDelegateProtocol
import platform.MapKit.MKMarkerAnnotationView
import platform.MapKit.MKPointAnnotation
import platform.UIKit.UIColor
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject

/**
 * The Explore map on iOS, drawn by MapKit.
 *
 * Apple's map rather than Google's, which is the one place this feature is not literally
 * the same on both platforms. There is no Google Maps SDK for iOS that comes without its
 * own key, its own bundle and its own attribution requirements, and MapKit is already
 * linked into every iOS app for free. The *places* are identical either way — they come
 * from OpenStreetMap and Wikipedia over plain HTTP, which does not care what draws them —
 * so a marker is in the same spot with the same description on both, and only the
 * cartography underneath differs.
 *
 * The interop view keeps Compose's default [androidx.compose.ui.viewinterop.UIKitInteropInteractionMode.Cooperative]
 * touch handling rather than `NonCooperative`. Cooperative costs the first ~150 ms of a
 * pan, which MapKit replays; NonCooperative would give MapKit every touch that lands on
 * it first, and the sheet's scrim lies directly on top of this view. Losing the ability
 * to dismiss the sheet by tapping beside it is a worse trade than a barely perceptible
 * delay when starting to drag the map.
 *
 * **The `update` block is on the hot path.** It runs on every recomposition that reaches
 * this composable, and every line of it crosses into Objective-C. Each `sync` below is
 * therefore guarded by what it last applied, so an update that changed nothing this view
 * cares about — a sheet opening, an article arriving — costs four comparisons instead of
 * a diff over forty places and a sweep of every annotation on screen.
 */
@OptIn(ExperimentalForeignApi::class)
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
    // Held across recompositions for two reasons: MKMapView keeps only a weak reference
    // to its delegate, so a per-composition one would be collected before the first tap
    // arrived, and it is also where the annotations currently on the map are tracked.
    val controller = remember { PlaceMapController() }

    // Re-read rather than captured: the delegate outlives any one composition, and a
    // callback captured at construction would keep calling the first composition's
    // view model long after the screen had been recreated.
    //
    // In a `SideEffect` rather than straight in the composition body, because a
    // composition can be started and then abandoned — and an assignment made in one that
    // was thrown away would leave the map reporting taps to a screen that never appeared.
    SideEffect {
        controller.onPlaceSelected = onPlaceSelected
        controller.onCameraApplied = onCameraApplied
    }

    UIKitView(
        factory = {
            // Recorded where the decision is made. With neither a request nor a fix to
            // open on, the map opens on the fallback below and the first request to
            // arrive is a correction rather than a journey — see `syncCamera`.
            controller.isAwaitingFirstFix = camera == null && userLocation == null
            MKMapView().apply {
                delegate = controller
                showsCompass = true
                showsScale = false
                // Off until a fix exists, matching Android; `syncChrome` turns it on.
                // Switching it on earlier makes MapKit raise its own permission prompt,
                // and the screen has already asked in the app's own words.
                showsUserLocation = false
                pitchEnabled = false
                // An MKMapView with no region set opens on the whole globe and starts
                // fetching tiles for it. This view is now created before the first fix
                // arrives — see `ExploreScreen`, which composes the map while its loading
                // cover is still up — so without this the head start would be spent
                // downloading a picture of the Pacific. The same fallback Android opens on.
                setRegion(
                    regionAround(
                        center = camera?.target ?: userLocation ?: GeoPoint.HANOI_CENTER,
                        zoom = camera?.zoom ?: MapZoom.NEIGHBOURHOOD,
                    ),
                    animated = false,
                )
            }
        },
        modifier = modifier,
        update = { mapView ->
            controller.syncChrome(mapView, isDarkTheme = isDarkTheme, hasFix = userLocation != null)
            controller.syncAnnotations(mapView, places)
            controller.syncSelection(mapView, selectedPlaceId)
            controller.syncCamera(mapView, camera)
        },
        onRelease = { mapView ->
            // The delegate reference is weak, but the map view is not released the
            // instant this runs — clearing it stops a callback arriving against a
            // controller whose composition has already gone.
            mapView.delegate = null
        },
    )
}

/**
 * An annotation that remembers which place it stands for.
 *
 * MapKit hands the delegate the annotation that was tapped and nothing else, so the id
 * has to travel on the annotation itself. Subclassing is the only place to put it that
 * does not show: `title` and `subtitle` are the two other candidates and both are drawn.
 */
@OptIn(ExperimentalForeignApi::class)
private class PlaceAnnotation(val placeId: String) : MKPointAnnotation()

/**
 * The map's delegate, and the record of what is currently on it.
 *
 * Annotations are diffed by id rather than cleared and re-added on every update. A
 * refresh usually returns most of the same places, and removing an annotation the
 * traveller has open would close their sheet and drop the selection under them.
 */
@OptIn(ExperimentalForeignApi::class)
private class PlaceMapController : NSObject(), MKMapViewDelegateProtocol {

    var onPlaceSelected: (String) -> Unit = {}
    var onCameraApplied: () -> Unit = {}

    /** True while [syncSelection] is driving MapKit, so its callback can be ignored. */
    private var isApplyingSelection = false

    private val annotations = mutableMapOf<String, PlaceAnnotation>()
    private val colorsById = mutableMapOf<String, UIColor>()
    private var selectedId: String? = null
    private var lastCameraRequestId: Long? = null

    /** Set by the factory: this map opened on the fallback region. See [syncCamera]. */
    var isAwaitingFirstFix = false

    /** What each `sync` last pushed into MapKit, so it can decline to push it twice. */
    private var lastPlaces: List<NearbyPlace>? = null
    private var lastDarkTheme: Boolean? = null
    private var lastShowsUserLocation: Boolean? = null

    fun syncChrome(mapView: MKMapView, isDarkTheme: Boolean, hasFix: Boolean) {
        if (lastDarkTheme != isDarkTheme) {
            lastDarkTheme = isDarkTheme
            mapView.overrideUserInterfaceStyle = if (isDarkTheme) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
            } else {
                UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        }
        if (lastShowsUserLocation != hasFix) {
            lastShowsUserLocation = hasFix
            // Only once a fix exists, matching Android: switching it on earlier makes
            // MapKit raise its own permission prompt, and the screen has already asked
            // in the app's own words.
            mapView.showsUserLocation = hasFix
        }
    }

    fun syncAnnotations(mapView: MKMapView, places: List<NearbyPlace>) {
        // Reference identity, not equality, and it is exact rather than a heuristic: the
        // view model replaces this list wholesale on every load, so the same instance is
        // the same markers. Without the guard the work below — a map allocation, two set
        // differences, and a `UIColor` plus a bridged `setCoordinate` per place — ran on
        // every recomposition that reached the update block, which includes every one
        // that only moved a spinner or filled in a sheet.
        if (places === lastPlaces) return
        lastPlaces = places

        val desired = places.associateBy { it.id }

        (annotations.keys - desired.keys).forEach { goneId ->
            annotations.remove(goneId)?.let(mapView::removeAnnotation)
            colorsById.remove(goneId)
        }

        desired.forEach { (id, place) ->
            colorsById[id] = markerColor(place.category).toUIColor()
            val existing = annotations[id]
            if (existing == null) {
                val annotation = PlaceAnnotation(id).apply {
                    setCoordinate(place.location.toCoordinate())
                    setTitle(place.name)
                }
                annotations[id] = annotation
                mapView.addAnnotation(annotation)
            } else {
                // A place does not move, but the same id can come back from a refresh
                // with a corrected position, and re-adding would duplicate the pin.
                existing.setCoordinate(place.location.toCoordinate())
            }
        }
    }

    fun syncSelection(mapView: MKMapView, placeId: String?) {
        if (placeId == selectedId) return
        selectedId = placeId

        if (placeId == null) {
            mapView.selectedAnnotations
                .filterIsInstance<PlaceAnnotation>()
                .forEach { mapView.deselectAnnotation(it, animated = true) }
        } else {
            // Guarded, because `selectAnnotation` makes MapKit re-enter this object's own
            // `didSelectAnnotationView`. Unguarded, closing and reopening a sheet reports
            // a fresh tap back up to the view model, which cancels and restarts the
            // details request the sheet had already begun.
            isApplyingSelection = true
            annotations[placeId]?.let { mapView.selectAnnotation(it, animated = true) }
            isApplyingSelection = false
        }
        // Outside the branch, deliberately. Sitting inside the selection branch, the
        // deselect path returned before reaching it and every pin stayed at
        // `UNSELECTED_ALPHA` for ever — the whole map faded, with nothing selected,
        // until each annotation happened to be recycled off screen and back.
        refreshMarkerStyles(mapView)
    }

    fun syncCamera(mapView: MKMapView, camera: MapCamera?) {
        val request = camera ?: return
        if (request.requestId == lastCameraRequestId) return
        // A map that opened on the fallback is being corrected, not travelled: this view
        // is built before the first fix arrives, and flying there would carry the
        // traveller in from Hanoi over a city they may be nowhere near. Every request
        // after it — and every request at all when the tab is reopened with a fix already
        // in state — is a move they asked for, and those still animate.
        val isCorrection = isAwaitingFirstFix
        isAwaitingFirstFix = false
        lastCameraRequestId = request.requestId

        mapView.setRegion(regionAround(request.target, request.zoom), animated = !isCorrection)
        // `setRegion` has no completion callback, and the animation is cosmetic — the
        // request is carried out the moment the region is set, so retiring it here is
        // both correct and the only option MapKit offers.
        onCameraApplied()
    }

    override fun mapView(
        mapView: MKMapView,
        viewForAnnotation: MKAnnotationProtocol,
    ): MKAnnotationView? {
        // Null for anything that is not one of ours — most importantly the user's own
        // location, which MapKit draws as its blue dot only if it is left alone.
        val annotation = viewForAnnotation as? PlaceAnnotation ?: return null

        val view = mapView.dequeueReusableAnnotationViewWithIdentifier(REUSE_IDENTIFIER)
            as? MKMarkerAnnotationView
            ?: MKMarkerAnnotationView(annotation = annotation, reuseIdentifier = REUSE_IDENTIFIER)

        view.annotation = annotation
        view.applyStyle(annotation.placeId)
        return view
    }

    override fun mapView(mapView: MKMapView, didSelectAnnotationView: MKAnnotationView) {
        // Ignored while this object is the one doing the selecting: MapKit reports a
        // programmatic `selectAnnotation` through the same callback as a real tap.
        if (isApplyingSelection) return
        val annotation = didSelectAnnotationView.annotation as? PlaceAnnotation ?: return
        selectedId = annotation.placeId
        refreshMarkerStyles(mapView)
        onPlaceSelected(annotation.placeId)
    }

    /** Pushes the current selection into whatever marker views are on screen right now. */
    private fun refreshMarkerStyles(mapView: MKMapView) {
        annotations.values.forEach { annotation ->
            (mapView.viewForAnnotation(annotation) as? MKMarkerAnnotationView)
                ?.applyStyle(annotation.placeId)
        }
    }

    private fun MKMarkerAnnotationView.applyStyle(placeId: String) {
        markerTintColor = colorsById[placeId]
        // The sheet is what shows this place, so a callout would be the same information
        // twice — and it would cover the pin the traveller just aimed at.
        canShowCallout = false
        val isSelected = placeId == selectedId
        // MapKit hides overlapping markers by priority rather than drawing them on top of
        // each other. `Required` keeps the open one visible however dense the cluster.
        displayPriority = if (isSelected) {
            MKFeatureDisplayPriorityRequired
        } else {
            MKFeatureDisplayPriorityDefaultHigh
        }
        alpha = if (selectedId == null || isSelected) 1.0 else UNSELECTED_ALPHA
    }
}

/*
 * At file scope rather than in a companion inside the controller.
 *
 * Kotlin/Native rejects "Fields are not supported for Companion of subclass of ObjC
 * type": a companion object needs a static field to hold its own instance, and an
 * Objective-C subclass has no place to put one. Constants belonging to a delegate
 * therefore live beside it.
 */
private const val REUSE_IDENTIFIER = "saola.place"

private const val UNSELECTED_ALPHA = 0.55

/**
 * How much ground to show, in metres, matching the zoom levels Android is given.
 *
 * MapKit measures a region by the distance it spans rather than by a zoom level, which
 * is why [MapZoom] is a name on both sides instead of a number on one.
 */
private val MapZoom.spanMeters: Double
    get() = when (this) {
        MapZoom.NEIGHBOURHOOD -> 6_000.0
        MapZoom.PLACE -> 900.0
    }

/** The square of ground MapKit should frame around a point. */
@OptIn(ExperimentalForeignApi::class)
private fun regionAround(center: GeoPoint, zoom: MapZoom): CValue<MKCoordinateRegion> =
    MKCoordinateRegionMakeWithDistance(
        centerCoordinate = center.toCoordinate(),
        latitudinalMeters = zoom.spanMeters,
        longitudinalMeters = zoom.spanMeters,
    )

@OptIn(ExperimentalForeignApi::class)
private fun GeoPoint.toCoordinate(): CValue<CLLocationCoordinate2D> =
    CLLocationCoordinate2DMake(latitude, longitude)

private fun Color.toUIColor(): UIColor = UIColor(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
