package com.duylt.trave.vietlensai.feature.sovereignty

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.domain.model.Archipelago
import com.duylt.trave.vietlensai.domain.model.GeoBounds
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.PI

/**
 * The Biển Đông as the statement page draws it: Việt Nam, the shores that face it,
 * and Hoàng Sa and Trường Sa in the water where they actually are.
 *
 * Deliberately not the passport's map. That one is a collection the traveller fills
 * in, so it fits itself to the mainland and files the archipelagos into inset boxes
 * down the side — the convention for a map of the country. This one exists to show
 * *where the islands are*, which means the sea has to be drawn at its real size,
 * with the archipelagos in place and coastlines on the far side of them for scale.
 *
 * Nothing here pans or zooms. It is a figure in a page of prose, not an instrument.
 */
class RegionMap(
    val frame: GeoBounds,
    /** Dissolved from the app's own province outlines, so it is the country the passport draws. */
    val vietnam: List<DoubleArray>,
    /** Context only: the coasts that enclose this sea. */
    val neighbours: List<DoubleArray>,
    val archipelagos: List<ArchipelagoMarker>,
) {
    /**
     * Width to height of the frame once projected.
     *
     * The card sizes itself to this rather than the drawing being fitted into a card
     * of some other shape: fit and there is a band of dead red down one side, fill
     * and the frame is cropped — and the frame was chosen so that nothing in it is
     * expendable.
     */
    val aspectRatio: Float
        get() {
            val cosLatitude = cos(((frame.minLatitude + frame.maxLatitude) / 2.0).toRadians())
            return (frame.longitudeSpan * cosLatitude / frame.latitudeSpan).toFloat()
        }
}

/** One archipelago's islands, as the points a symbol is drawn at. */
class ArchipelagoMarker(
    val archipelago: Archipelago,
    /** Flat `[lon, lat, …]`, one pair per island worth drawing at this scale. */
    val islands: DoubleArray,
)

/**
 * Reads `composeResources/files/sovereignty_map.json`, built by `tools/region/build_region.py`.
 *
 * `org.json` used to do this, on the grounds that the app module had no serialization plugin.
 * That reasoning no longer holds — `org.json` is an Android-only library and this screen now
 * has to compile for iOS too — so the file is described as a DTO like every other asset.
 */
fun parseRegionMap(text: String): RegionMap {
    val dto = regionJson.decodeFromString(RegionMapDto.serializer(), text)
    return RegionMap(
        frame = GeoBounds(
            minLongitude = dto.frame.getOrElse(0) { 0.0 },
            minLatitude = dto.frame.getOrElse(1) { 0.0 },
            maxLongitude = dto.frame.getOrElse(2) { 0.0 },
            maxLatitude = dto.frame.getOrElse(3) { 0.0 },
        ),
        // Rings shorter than three vertices cannot be drawn and are dropped rather than
        // producing a degenerate path.
        vietnam = dto.vietnam.filter { it.size >= 6 },
        neighbours = dto.neighbours.filter { it.size >= 6 },
        archipelagos = listOf(
            ArchipelagoMarker(Archipelago.HOANG_SA, dto.hoangSa),
            ArchipelagoMarker(Archipelago.TRUONG_SA, dto.truongSa),
        ),
    )
}

private val regionJson = Json { ignoreUnknownKeys = true }

/** `[minLon, minLat, maxLon, maxLat]` plus flat `[lon, lat, …]` rings, as the tool writes them. */
@Serializable
private class RegionMapDto(
    val frame: DoubleArray = DoubleArray(0),
    val vietnam: List<DoubleArray> = emptyList(),
    val neighbours: List<DoubleArray> = emptyList(),
    val hoangSa: DoubleArray = DoubleArray(0),
    val truongSa: DoubleArray = DoubleArray(0),
)

@Composable
fun SovereigntyMapCanvas(
    map: RegionMap,
    countryLabel: String,
    seaLabel: String,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val projection = RegionProjection(map.frame, size)

        drawLand(map.neighbours, projection, NeighbourFill, NeighbourStroke)
        drawLand(map.vietnam, projection, VietnamFill, VietnamStroke)

        drawPlaceLabel(
            text = seaLabel.uppercase(),
            centre = projection.at(SEA_LABEL_LONGITUDE, SEA_LABEL_LATITUDE),
            style = seaLabelStyle(),
            textMeasurer = textMeasurer,
        )
        drawPlaceLabel(
            text = countryLabel.uppercase(),
            centre = projection.at(COUNTRY_LABEL_LONGITUDE, COUNTRY_LABEL_LATITUDE),
            style = countryLabelStyle(),
            textMeasurer = textMeasurer,
        )

        map.archipelagos.forEach { drawArchipelago(it, projection, textMeasurer) }
    }
}

private fun DrawScope.drawLand(
    rings: List<DoubleArray>,
    projection: RegionProjection,
    fill: Color,
    stroke: Color,
) {
    val path = Path()
    rings.forEach { ring ->
        val vertices = ring.size / 2
        if (vertices < 3) return@forEach
        for (i in 0 until vertices) {
            val point = projection.at(ring[i * 2], ring[i * 2 + 1])
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
    }
    drawPath(path, fill)
    drawPath(path, stroke, style = Stroke(width = COAST_STROKE_DP.dp.toPx()))
}

/**
 * A dashed ring around the islands, the islands themselves as dots, and the name.
 *
 * Dotted at this scale for the same reason the passport stipples them: Hoàng Sa's
 * islets are under a kilometre across in a two-hundred-kilometre archipelago, so a
 * dot is not a simplification of the island, it is the only way to show one at all.
 * The ring is what says the group is a group.
 */
private fun DrawScope.drawArchipelago(
    marker: ArchipelagoMarker,
    projection: RegionProjection,
    textMeasurer: TextMeasurer,
) {
    val points = (0 until marker.islands.size / 2).map { i ->
        projection.at(marker.islands[i * 2], marker.islands[i * 2 + 1])
    }
    if (points.isEmpty()) return

    val bounds = points.fold(Rect(points[0], points[0])) { rect, point ->
        Rect(
            left = minOf(rect.left, point.x),
            top = minOf(rect.top, point.y),
            right = maxOf(rect.right, point.x),
            bottom = maxOf(rect.bottom, point.y),
        )
    }
    val centre = bounds.center
    val radius = max(
        max(bounds.width, bounds.height) / 2f + RING_PADDING_DP.dp.toPx(),
        RING_MIN_RADIUS_DP.dp.toPx(),
    )

    drawCircle(
        color = Marigold.copy(alpha = RING_ALPHA),
        radius = radius,
        center = centre,
        style = Stroke(
            width = RING_STROKE_DP.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(RING_DASH_DP.dp.toPx(), RING_DASH_DP.dp.toPx()),
            ),
        ),
    )
    points.forEach { drawCircle(Marigold, radius = ISLAND_DOT_DP.dp.toPx(), center = it) }

    // Above the ring for Hoàng Sa, below it for Trường Sa — each label pushed towards
    // the nearer edge of the frame, so neither lands in the middle of the sea where
    // the sea's own name is.
    val above = centre.y < projection.height / 2f
    val gap = radius + LABEL_GAP_DP.dp.toPx()
    val label = textMeasurer.measure(
        text = marker.archipelago.vietnameseName,
        style = archipelagoLabelStyle(),
        layoutDirection = LayoutDirection.Ltr,
        density = this,
    )
    drawText(
        textLayoutResult = label,
        topLeft = Offset(
            x = centre.x - label.size.width / 2f,
            y = if (above) centre.y - gap - label.size.height else centre.y + gap,
        ),
    )
}

private fun DrawScope.drawPlaceLabel(
    text: String,
    centre: Offset,
    style: TextStyle,
    textMeasurer: TextMeasurer,
) {
    val measured: TextLayoutResult = textMeasurer.measure(
        text = text,
        style = style,
        layoutDirection = LayoutDirection.Ltr,
        density = this,
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            x = centre.x - measured.size.width / 2f,
            y = centre.y - measured.size.height / 2f,
        ),
    )
}

/**
 * Equirectangular with a cosine correction at the frame's mid-latitude — the same
 * projection the passport map uses, for the same reason: over this few degrees it is
 * indistinguishable from a conformal one and it is two lines long.
 *
 * There is no clamping or inversion here. Nothing on this map is tapped or dragged.
 */
private class RegionProjection(private val frame: GeoBounds, size: Size) {

    private val cosLatitude = cos(((frame.minLatitude + frame.maxLatitude) / 2.0).toRadians())
    private val worldWidth = frame.longitudeSpan * cosLatitude
    private val worldHeight = frame.latitudeSpan

    private val scale: Double = if (worldWidth <= 0.0 || worldHeight <= 0.0) {
        1.0
    } else {
        minOf(size.width / worldWidth, size.height / worldHeight)
    }
    private val originX = (size.width - worldWidth * scale) / 2.0
    private val originY = (size.height - worldHeight * scale) / 2.0

    val height: Float = size.height

    fun at(longitude: Double, latitude: Double): Offset = Offset(
        x = (originX + (longitude - frame.minLongitude) * cosLatitude * scale).toFloat(),
        // Latitude grows north, screen y grows down.
        y = (originY + (frame.maxLatitude - latitude) * scale).toFloat(),
    )
}

private fun archipelagoLabelStyle() = TextStyle(
    color = PaperCream,
    fontSize = 11.sp,
    fontWeight = FontWeight.SemiBold,
)

private fun seaLabelStyle() = TextStyle(
    color = PaperCream.copy(alpha = 0.50f),
    fontSize = 9.sp,
    fontFamily = FontFamily.Monospace,
    letterSpacing = 1.6.sp,
)

private fun countryLabelStyle() = TextStyle(
    color = PaperCream.copy(alpha = 0.75f),
    fontSize = 9.sp,
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    letterSpacing = 1.6.sp,
)

// Cream at two strengths rather than two different hues: the whole card is one
// lacquer red, and land that drifted towards another colour would read as a second
// palette rather than as the same map twice over.
private val NeighbourFill = PaperCream.copy(alpha = 0.20f)
private val NeighbourStroke = PaperCream.copy(alpha = 0.26f)
private val VietnamFill = PaperCream.copy(alpha = 0.40f)
private val VietnamStroke = Marigold.copy(alpha = 0.55f)

/**
 * The north, over the delta.
 *
 * The country is five degrees wide there and barely one at its waist, so this is the
 * only place a name this long sits inside its own borders. Centred on the middle of
 * the country instead, it lands half over Laos and reads as labelling that.
 */
private const val COUNTRY_LABEL_LONGITUDE = 105.1
private const val COUNTRY_LABEL_LATITUDE = 21.4

/** Open water, clear of both archipelagos and of every coast. */
private const val SEA_LABEL_LONGITUDE = 113.8
private const val SEA_LABEL_LATITUDE = 13.2

private const val COAST_STROKE_DP = 0.8f
private const val RING_STROKE_DP = 1f
private const val RING_DASH_DP = 3f
private const val RING_PADDING_DP = 7f
private const val RING_MIN_RADIUS_DP = 14f
private const val RING_ALPHA = 0.8f
private const val ISLAND_DOT_DP = 1.8f
private const val LABEL_GAP_DP = 5f

/** `Math.toRadians` is java.lang-only; the conversion itself is one multiplication. */
private fun Double.toRadians(): Double = this * PI / 180.0
