package com.evora.technologies.saola.feature.passport

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.platform.LocalDensity
import com.evora.technologies.saola.domain.model.Archipelago
import com.evora.technologies.saola.domain.model.GeoBounds
import com.evora.technologies.saola.domain.model.PassportStamp
import com.evora.technologies.saola.domain.model.Province
import com.evora.technologies.saola.feature.passport.component.MapPlacement
import com.evora.technologies.saola.feature.passport.component.ProvinceHandle
import com.evora.technologies.saola.feature.passport.component.ProvinceSemanticsOverlay
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.passport_map_a11y
import com.evora.technologies.saola.resources.passport_province_a11y
import com.evora.technologies.saola.resources.passport_province_a11y_open
import com.evora.technologies.saola.resources.passport_status_locked
import com.evora.technologies.saola.resources.passport_status_unlocked
import org.jetbrains.compose.resources.stringResource
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI

/**
 * Vietnam as a mosaic: 34 provinces, each visited one filled with the traveller's
 * own photo, clipped to the province's real outline.
 *
 * There are no jigsaw teeth drawn anywhere. Administrative borders already
 * interlock — that *is* the puzzle — and adding decorative tabs would fight the
 * geography rather than express it.
 *
 * Hoàng Sa and Trường Sa are drawn in inset boxes down the right-hand side rather
 * than in place. Drawn where they actually are, they push the map's extent 3.4°
 * further east and flatten the mainland from an aspect of 0.475 to 0.818 — pinning
 * the part the traveller actually collects into little over half the screen to make
 * room for open sea. Every Vietnamese map solves this the same way. The insets do
 * not pan or zoom with the mainland: they are a separate reference frame, which is
 * also the convention.
 *
 * At country zoom a province is 40–80 px wide on a phone, so a photo inside one
 * reads as a patch of colour rather than as a picture. That is the intended effect;
 * pinch-zoom and the detail sheet are what make an individual photo legible.
 */
@Composable
fun VietnamMapCanvas(
    stamps: List<PassportStamp>,
    covers: Map<String, CoverPhoto>,
    selectedProvinceId: String?,
    onSelect: (String?) -> Unit,
    lockedFill: Color,
    lockedStroke: Color,
    unlockedStroke: Color,
    selectedStroke: Color,
    insetLabel: Color,
    modifier: Modifier = Modifier,
) {
    // Saveable so a rotation mid-journey does not throw the traveller back out to
    // country zoom.
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }

    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    val layout = remember(stamps, canvasSize, density) {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) null
        else MapLayout(stamps, canvasSize, density)
    }

    // Paths are built in unmagnified canvas space and reused across pan and zoom,
    // which are applied as a transform. Rebuilding 9,151 vertices on every gesture
    // frame would be the one thing certain to make this feel slow.
    val shapes = remember(layout, stamps) {
        layout?.let { l -> stamps.associate { it.province.id to it.province.toShape(l.projection) } }
            ?: emptyMap()
    }

    val mapSummary = stringResource(
        Res.string.passport_map_a11y,
        stamps.count { it.isUnlocked },
        stamps.size,
    )

    // The two archipelago insets first, then the mainland, because that is the order the map
    // is read in Vietnamese convention and the order a screen reader will announce them.
    val geometry = remember(layout, stamps, shapes) { layout.handleGeometry(stamps, shapes) }
    val visited = stringResource(Res.string.passport_status_unlocked)
    val notVisited = stringResource(Res.string.passport_status_locked)
    val openLabel = stringResource(Res.string.passport_province_a11y_open)
    val handles = geometry.map { piece ->
        ProvinceHandle(
            id = piece.provinceId,
            label = stringResource(
                Res.string.passport_province_a11y,
                piece.name,
                if (piece.isUnlocked) visited else notVisited,
            ),
            bounds = piece.bounds,
            isFixed = piece.isFixed,
        )
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // A Canvas does not clip its own drawing. Zoomed in, the transformed
                // country would paint straight over the progress header above it and the
                // hint below.
                .clipToBounds()
                // The whole picture in one sentence, focused before the provinces under it. The
                // progress bar above the map says the same numbers to a sighted traveller; a
                // screen reader arriving at a map with no label at all learns nothing from
                // thirty-four province names it has not been told are a map of Vietnam.
                .semantics {
                    contentDescription = mapSummary
                }
                // Size is captured here rather than from the draw scope: assigning state
                // during a draw pass schedules another one, and the map would redraw
                // forever.
                .onSizeChanged { canvasSize = it.toSize() }
                // Keyed on the layout, not Unit: the first composition has no size yet,
                // so a block launched once would capture a null layout for ever and
                // silently disable panning.
                .pointerInput(layout) {
                    val projection = layout?.projection
                    detectTransformGestures { centroid, pan, gestureZoom, _ ->
                        val current = Offset(offsetX, offsetY)
                        val newZoom = (zoom * gestureZoom).coerceIn(MIN_ZOOM, MAX_ZOOM)
                        // Keep the point under the fingers pinned while the scale changes.
                        val moved = centroid - (centroid - current) * (newZoom / zoom) + pan
                        val settled = projection.clamp(moved, newZoom, layout?.mapSize ?: Size.Zero)
                        zoom = newZoom
                        offsetX = settled.x
                        offsetY = settled.y
                    }
                }
                .pointerInput(layout, stamps) {
                    detectTapGestures { tap ->
                        val l = layout ?: return@detectTapGestures

                        // Insets are checked first and hit as whole boxes. They sit in a
                        // fixed frame, and the islands inside them are a few pixels
                        // across — asking for a fingertip on one would make the
                        // archipelagos decorative rather than selectable.
                        val inset = l.insets.firstOrNull { it.hitRect.contains(tap) }
                        if (inset != null) {
                            onSelect(inset.ownerProvinceId)
                            return@detectTapGestures
                        }

                        val settled = l.projection.clamp(Offset(offsetX, offsetY), zoom, l.mapSize)
                        val world = (tap - settled) / zoom
                        val lon = l.projection.longitudeAt(world.x)
                        val lat = l.projection.latitudeAt(world.y)
                        onSelect(stamps.firstOrNull { it.province.contains(lon, lat) }?.province?.id)
                    }
                },
        ) {
            val l = layout ?: return@Canvas

            // Re-clamped here rather than trusting the stored offset: the canvas resizes
            // when the empty hint appears on first unlock, and on rotation, either of
            // which can leave a previously legal offset pointing off the map.
            //
            // The pan is read *here*, in the draw scope, rather than into a `val` above the
            // `Canvas`. Read during composition it made every frame of a drag recompose the whole
            // map — which cost nothing visible while this drew and nothing else, and would now
            // re-resolve thirty-six accessibility labels per frame.
            val settled = l.projection.clamp(Offset(offsetX, offsetY), zoom, l.mapSize)

            withTransform({
                translate(settled.x, settled.y)
                scale(zoom, zoom, pivot = Offset.Zero)
            }) {
                // Borders keep a constant on-screen width no matter how far in the
                // traveller has zoomed; scaling them would turn a tight cluster of delta
                // provinces into a solid block of outline.
                val hairline = BORDER_DP.dp.toPx() / zoom
                val selectedWidth = SELECTED_BORDER_DP.dp.toPx() / zoom

                stamps.forEach { stamp ->
                    val shape = shapes[stamp.province.id] ?: return@forEach
                    val cover = covers[stamp.province.id]?.image

                    if (cover != null) {
                        // Each ring is filled from the photo independently rather than
                        // stretching one copy across the whole province, so a province
                        // with coastal islands shows the same picture in each piece
                        // instead of an unrelated band from the photo's far edge.
                        shape.parts.forEach { part ->
                            clipPath(part.path) { drawCover(cover, part.bounds) }
                        }
                    } else {
                        drawPath(shape.outline, lockedFill)
                    }
                }

                // Every outline goes on top of every fill, so a border is never half
                // hidden under the neighbouring province's photo.
                stamps.forEach { stamp ->
                    val shape = shapes[stamp.province.id] ?: return@forEach
                    val isSelected = stamp.province.id == selectedProvinceId
                    drawPath(
                        path = shape.outline,
                        color = when {
                            isSelected -> selectedStroke
                            stamp.isUnlocked -> unlockedStroke
                            else -> lockedStroke
                        },
                        style = Stroke(width = if (isSelected) selectedWidth else hairline),
                    )
                }
            }

            // Outside the transform: the insets keep their place and scale while the
            // mainland moves under the finger.
            l.insets.forEach { inset ->
                drawInset(
                    inset = inset,
                    cover = inset.ownerProvinceId?.let { covers[it]?.image },
                    isSelected = inset.ownerProvinceId != null &&
                        inset.ownerProvinceId == selectedProvinceId,
                    textMeasurer = textMeasurer,
                    lockedFill = lockedFill,
                    lockedStroke = lockedStroke,
                    unlockedStroke = unlockedStroke,
                    selectedStroke = selectedStroke,
                    labelColor = insetLabel,
                )
            }
        }

        // Over the canvas and drawing nothing. See `ProvinceSemanticsOverlay` for why these
        // are semantics without a pointer input, and why that leaves every existing gesture
        // exactly as it was.
        ProvinceSemanticsOverlay(
            handles = handles,
            placement = {
                val l = layout
                MapPlacement(
                    zoom = zoom,
                    offset = l?.projection.clamp(Offset(offsetX, offsetY), zoom, l?.mapSize ?: Size.Zero),
                )
            },
            openLabel = openLabel,
            onSelect = onSelect,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * Every province and inset as a labelled rectangle, in the canvas's unmagnified space.
 *
 * A province is drawn as one or more rings — Quảng Ninh has islands, Cà Mau has a delta — and
 * what a screen reader can be given is one rectangle, so the rings are unioned. Rings are what
 * the photo is fitted to and rings are what the outline strokes; neither of those wants a
 * bounding box, which is why this is computed here rather than kept on [ProvinceShape].
 *
 * An inset with no owner is dropped rather than labelled: nothing selects it, so a node there
 * would be a control that does nothing.
 */
private fun MapLayout?.handleGeometry(
    stamps: List<PassportStamp>,
    shapes: Map<String, ProvinceShape>,
): List<ProvinceGeometry> {
    val layout = this ?: return emptyList()

    val insets = layout.insets.mapNotNull { inset ->
        val owner = inset.ownerProvinceId ?: return@mapNotNull null
        ProvinceGeometry(
            provinceId = owner,
            name = inset.archipelago.vietnameseName,
            isUnlocked = inset.isUnlocked,
            bounds = inset.hitRect,
            isFixed = true,
        )
    }

    val mainland = stamps.mapNotNull { stamp ->
        val parts = shapes[stamp.province.id]?.parts.orEmpty()
        if (parts.isEmpty()) return@mapNotNull null
        ProvinceGeometry(
            provinceId = stamp.province.id,
            name = stamp.province.displayName,
            isUnlocked = stamp.isUnlocked,
            bounds = Rect(
                left = parts.minOf { it.bounds.left },
                top = parts.minOf { it.bounds.top },
                right = parts.maxOf { it.bounds.right },
                bottom = parts.maxOf { it.bounds.bottom },
            ),
        )
    }

    return insets + mainland
}

/** One labelled rectangle before its text has been resolved. */
private class ProvinceGeometry(
    val provinceId: String,
    val name: String,
    val isUnlocked: Boolean,
    val bounds: Rect,
    val isFixed: Boolean = false,
)

/**
 * Everything the map's geometry depends on: where the mainland is drawn, and where
 * the two archipelago boxes sit beside it.
 *
 * Computed together because they share the canvas — the insets take a column down
 * the right and the mainland is fitted into what is left, so neither can be worked
 * out without the other.
 */
private class MapLayout(stamps: List<PassportStamp>, canvas: Size, density: Density) {

    val mapSize: Size
    val projection: MapProjection
    val insets: List<InsetBox>

    init {
        val columnWidth = with(density) {
            min(canvas.width * INSET_COLUMN_FRACTION, INSET_COLUMN_MAX_DP.dp.toPx())
        }
        val gutter = with(density) { INSET_GUTTER_DP.dp.toPx() }
        val labelHeight = with(density) { INSET_LABEL_HEIGHT_DP.dp.toPx() }
        val minIslandRadius = with(density) { INSET_MIN_ISLAND_DP.dp.toPx() }

        mapSize = Size(canvas.width - columnWidth, canvas.height)
        projection = MapProjection(stamps.mainlandBounds(), mapSize)

        // Two square boxes stacked in the column, in their real north-to-south order
        // so the arrangement still reads as a map rather than as a legend.
        val boxSide = columnWidth - gutter * 2f
        val stackHeight = boxSide * 2f + labelHeight * 2f + gutter
        val top = ((canvas.height - stackHeight) / 2f).coerceAtLeast(gutter)
        val left = canvas.width - columnWidth + gutter

        insets = if (boxSide <= 0f) emptyList() else {
            Archipelago.entries.mapIndexed { index, archipelago ->
                val boxTop = top + index * (boxSide + labelHeight + gutter) + labelHeight
                InsetBox.of(
                    archipelago = archipelago,
                    rect = Rect(left, boxTop, left + boxSide, boxTop + boxSide),
                    labelHeight = labelHeight,
                    stamps = stamps,
                    minIslandRadius = minIslandRadius,
                )
            }
        }
    }
}

/** One archipelago's box: where it sits, who owns it, and its islands already placed. */
private class InsetBox(
    val archipelago: Archipelago,
    val rect: Rect,
    val labelHeight: Float,
    val ownerProvinceId: String?,
    val isUnlocked: Boolean,
    val islands: List<Path>,
    val islandBounds: List<PixelRect>,
) {
    /** The tap target: the box plus its label, so the whole thing is one affordance. */
    val hitRect: Rect get() = Rect(rect.left, rect.top - labelHeight, rect.right, rect.bottom)

    companion object {
        fun of(
            archipelago: Archipelago,
            rect: Rect,
            labelHeight: Float,
            stamps: List<PassportStamp>,
            minIslandRadius: Float,
        ): InsetBox {
            val owner = stamps.firstOrNull { stamp ->
                stamp.province.offshoreRings.any { Archipelago.of(it) == archipelago }
            }
            // Offshore rings are a mix of real islands and the administrative sea
            // areas the boundary data draws around them. The latter run to 1.3°
            // across — 145 km — and exist so that a photo taken anywhere in the
            // archipelago still resolves to its province. Drawn, they would be one
            // circle swallowing the whole box, so only islands get past here.
            val rings = owner?.province?.offshoreRings.orEmpty().filter { ring ->
                Archipelago.of(ring) == archipelago && ring.span() <= MAX_ISLAND_SPAN_DEGREES
            }

            val inner = archipelago.bounds
            val scale = min(
                rect.width / inner.longitudeSpan.toFloat(),
                rect.height / inner.latitudeSpan.toFloat(),
            )
            val originX = rect.left + (rect.width - inner.longitudeSpan.toFloat() * scale) / 2f
            val originY = rect.top + (rect.height - inner.latitudeSpan.toFloat() * scale) / 2f

            val paths = ArrayList<Path>(rings.size)
            val bounds = ArrayList<PixelRect>(rings.size)

            rings.forEach { ring ->
                val n = ring.size / 2
                if (n < 3) return@forEach
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -Float.MAX_VALUE
                var maxY = -Float.MAX_VALUE
                for (i in 0 until n) {
                    val px = originX + ((ring[i * 2] - inner.minLongitude) * scale).toFloat()
                    val py = originY + ((inner.maxLatitude - ring[i * 2 + 1]) * scale).toFloat()
                    if (px < minX) minX = px
                    if (px > maxX) maxX = px
                    if (py < minY) minY = py
                    if (py > maxY) maxY = py
                }

                // Drawn as symbols, not to scale. Hoàng Sa's islands are ~800 m
                // across inside a 200-km box — a faithful 0.4-pixel dot would be an
                // empty rectangle. Real maps stipple them for exactly this reason.
                val cx = (minX + maxX) / 2f
                val cy = (minY + maxY) / 2f
                val radius = max(max(maxX - minX, maxY - minY) / 2f, minIslandRadius)
                paths += Path().apply {
                    addOval(Rect(cx - radius, cy - radius, cx + radius, cy + radius))
                }
                bounds += PixelRect(cx - radius, cy - radius, cx + radius, cy + radius)
            }

            return InsetBox(
                archipelago = archipelago,
                rect = rect,
                labelHeight = labelHeight,
                ownerProvinceId = owner?.province?.id,
                isUnlocked = owner?.isUnlocked == true,
                islands = paths,
                islandBounds = bounds,
            )
        }
    }
}

private fun DrawScope.drawInset(
    inset: InsetBox,
    cover: ImageBitmap?,
    isSelected: Boolean,
    textMeasurer: TextMeasurer,
    lockedFill: Color,
    lockedStroke: Color,
    unlockedStroke: Color,
    selectedStroke: Color,
    labelColor: Color,
) {
    val border = when {
        isSelected -> selectedStroke
        inset.isUnlocked -> unlockedStroke
        else -> lockedStroke
    }

    // Dashed, which is how an inset is conventionally distinguished from a frame
    // drawn around real adjacent territory.
    drawRect(
        color = border,
        topLeft = Offset(inset.rect.left, inset.rect.top),
        size = Size(inset.rect.width, inset.rect.height),
        style = Stroke(
            width = (if (isSelected) SELECTED_BORDER_DP else BORDER_DP).dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(INSET_DASH_DP.dp.toPx(), INSET_DASH_DP.dp.toPx()),
            ),
        ),
    )

    // Islands are widened to a minimum symbol size, so one sitting near the edge can
    // overhang the frame. Clipping keeps the box a box.
    clipRect(inset.rect.left, inset.rect.top, inset.rect.right, inset.rect.bottom) {
        inset.islands.forEachIndexed { index, island ->
            if (cover != null) {
                clipPath(island) { drawCover(cover, inset.islandBounds[index]) }
            } else {
                drawPath(island, lockedFill)
            }
            drawPath(island, border, style = Stroke(width = INSET_ISLAND_BORDER_DP.dp.toPx()))
        }
    }

    val label = textMeasurer.measure(
        text = inset.archipelago.vietnameseName,
        style = TextStyle(fontSize = INSET_LABEL_SP.sp, color = labelColor),
        layoutDirection = LayoutDirection.Ltr,
        density = this,
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(
            x = inset.rect.left + (inset.rect.width - label.size.width) / 2f,
            y = inset.rect.top - label.size.height - INSET_LABEL_GAP_DP.dp.toPx(),
        ),
    )
}

/**
 * Draws [image] to fill [target] completely, cropped from the centre.
 *
 * Fills rather than fits: a letterboxed photo would leave the province's own
 * background showing through in the gaps and break the illusion that the picture
 * *is* the province.
 */
private fun DrawScope.drawCover(image: ImageBitmap, target: PixelRect) {
    if (target.width <= 0f || target.height <= 0f) return
    if (image.width <= 0 || image.height <= 0) return

    val scale = max(target.width / image.width, target.height / image.height)
    val drawnWidth = image.width * scale
    val drawnHeight = image.height * scale

    withTransform({
        translate(
            target.left + (target.width - drawnWidth) / 2f,
            target.top + (target.height - drawnHeight) / 2f,
        )
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawImage(image, topLeft = Offset.Zero)
    }
}

/**
 * Equirectangular projection with a cosine correction at the country's mid-latitude.
 *
 * Vietnam spans ~15° of latitude, where a proper conformal projection and this one
 * differ by less than the width of the stroke drawn around a province. What it buys
 * is an invertible pair of one-line functions, so a finger tap turns back into a
 * longitude and latitude and reuses the very same containment test as a GPS fix.
 */
private class MapProjection(private val bounds: GeoBounds, size: Size) {

    private val cosLatitude = cos(((bounds.minLatitude + bounds.maxLatitude) / 2.0).toRadians())
    private val worldWidth = bounds.longitudeSpan * cosLatitude
    private val worldHeight = bounds.latitudeSpan

    private val scale: Double = if (worldWidth <= 0.0 || worldHeight <= 0.0) 1.0 else {
        min(size.width / worldWidth, size.height / worldHeight)
    }
    private val originX = (size.width - worldWidth * scale) / 2.0
    private val originY = (size.height - worldHeight * scale) / 2.0

    /** Where the country actually sits inside the map area, before pan and zoom. */
    val contentLeft: Float get() = originX.toFloat()
    val contentTop: Float get() = originY.toFloat()
    val contentWidth: Float get() = (worldWidth * scale).toFloat()
    val contentHeight: Float get() = (worldHeight * scale).toFloat()

    fun x(longitude: Double): Float =
        (originX + (longitude - bounds.minLongitude) * cosLatitude * scale).toFloat()

    /** Latitude grows north, screen y grows down. */
    fun y(latitude: Double): Float =
        (originY + (bounds.maxLatitude - latitude) * scale).toFloat()

    fun longitudeAt(x: Float): Double =
        bounds.minLongitude + (x - originX) / (cosLatitude * scale)

    fun latitudeAt(y: Float): Double =
        bounds.maxLatitude - (y - originY) / scale
}

private class PixelRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/**
 * A province ready to draw: one outline for stroking, and each ring separately so a
 * photo can be fitted to the landmass it actually fills.
 */
private class ProvinceShape(val outline: Path, val parts: List<RingShape>)

private class RingShape(val path: Path, val bounds: PixelRect)

private fun Province.toShape(projection: MapProjection): ProvinceShape {
    val outline = Path()
    val parts = ArrayList<RingShape>(mainlandRings.size)

    mainlandRings.forEach { ring ->
        val vertices = ring.size / 2
        if (vertices < 3) return@forEach

        val part = Path()
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (i in 0 until vertices) {
            val px = projection.x(ring[i * 2])
            val py = projection.y(ring[i * 2 + 1])
            if (i == 0) part.moveTo(px, py) else part.lineTo(px, py)
            if (px < minX) minX = px
            if (px > maxX) maxX = px
            if (py < minY) minY = py
            if (py > maxY) maxY = py
        }
        part.close()

        outline.addPath(part)
        parts += RingShape(part, PixelRect(minX, minY, maxX, maxY))
    }

    return ProvinceShape(outline, parts)
}

/** Extent of the mainland, so the projection fits the part people actually collect. */
private fun List<PassportStamp>.mainlandBounds(): GeoBounds {
    if (isEmpty()) return GeoBounds(102.0, 8.0, 110.0, 24.0)
    var minLon = Double.MAX_VALUE
    var minLat = Double.MAX_VALUE
    var maxLon = -Double.MAX_VALUE
    var maxLat = -Double.MAX_VALUE
    forEach { stamp ->
        val b = stamp.province.mainlandBounds
        if (b.minLongitude < minLon) minLon = b.minLongitude
        if (b.minLatitude < minLat) minLat = b.minLatitude
        if (b.maxLongitude > maxLon) maxLon = b.maxLongitude
        if (b.maxLatitude > maxLat) maxLat = b.maxLatitude
    }
    return GeoBounds(minLon, minLat, maxLon, maxLat)
}

/**
 * Stops a pan from dragging the country off the edge of the screen.
 *
 * Clamped against the *drawn country*, not against the canvas. The projection
 * aspect-fits and centres, and Vietnam is far taller than it is wide, so a band of
 * the map area is always empty letterbox. Limits derived from the canvas would let a
 * drag at full zoom park that empty band over the whole viewport, leaving the
 * traveller staring at a blank rectangle with no hint that the map is off-screen.
 */
private fun MapProjection?.clamp(offset: Offset, zoom: Float, size: Size): Offset {
    if (this == null || size.width <= 0f || size.height <= 0f) return Offset.Zero
    return Offset(
        clampAxis(offset.x, zoom, contentLeft, contentWidth, size.width),
        clampAxis(offset.y, zoom, contentTop, contentHeight, size.height),
    )
}

private fun clampAxis(
    value: Float,
    zoom: Float,
    contentOrigin: Float,
    contentExtent: Float,
    viewport: Float,
): Float {
    val scaledOrigin = zoom * contentOrigin
    val scaledExtent = zoom * contentExtent
    // Smaller than the viewport: there is nothing to pan to, so keep it centred.
    if (scaledExtent <= viewport) return (viewport - scaledExtent) / 2f - scaledOrigin
    return value.coerceIn(viewport - (scaledOrigin + scaledExtent), -scaledOrigin)
}

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 8f
private const val BORDER_DP = 0.8f
private const val SELECTED_BORDER_DP = 2.5f

private const val INSET_COLUMN_FRACTION = 0.26f
private const val INSET_COLUMN_MAX_DP = 120f
private const val INSET_GUTTER_DP = 10f
private const val INSET_LABEL_HEIGHT_DP = 16f
private const val INSET_LABEL_GAP_DP = 3f
private const val INSET_LABEL_SP = 10f
private const val INSET_DASH_DP = 3f
private const val INSET_MIN_ISLAND_DP = 2f
private const val INSET_ISLAND_BORDER_DP = 0.6f

/** Above ~28 km an offshore ring is an administrative sea area, not an island. */
private const val MAX_ISLAND_SPAN_DEGREES = 0.25

/** Longest side of a ring's bounding box, in degrees. */
private fun DoubleArray.span(): Double {
    var minX = Double.MAX_VALUE
    var minY = Double.MAX_VALUE
    var maxX = -Double.MAX_VALUE
    var maxY = -Double.MAX_VALUE
    for (i in 0 until size / 2) {
        val x = this[i * 2]
        val y = this[i * 2 + 1]
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }
    return max(maxX - minX, maxY - minY)
}

/** `Math.toRadians` is java.lang-only; the conversion itself is one multiplication. */
private fun Double.toRadians(): Double = this * PI / 180.0
