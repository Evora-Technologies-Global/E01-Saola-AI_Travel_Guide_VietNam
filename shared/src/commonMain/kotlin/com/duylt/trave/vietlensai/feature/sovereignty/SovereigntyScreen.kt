package com.duylt.trave.vietlensai.feature.sovereignty

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.sovereignty_body
import com.duylt.trave.vietlensai.resources.sovereignty_body_secondary
import com.duylt.trave.vietlensai.resources.sovereignty_body_source
import com.duylt.trave.vietlensai.resources.sovereignty_close
import com.duylt.trave.vietlensai.resources.sovereignty_map_country
import com.duylt.trave.vietlensai.resources.sovereignty_map_sea
import com.duylt.trave.vietlensai.resources.sovereignty_note
import com.duylt.trave.vietlensai.resources.sovereignty_page_subtitle
import com.duylt.trave.vietlensai.resources.sovereignty_page_title
import com.duylt.trave.vietlensai.resources.sovereignty_understood
import com.duylt.trave.vietlensai.core.designsystem.component.CloseChip
import com.duylt.trave.vietlensai.core.designsystem.component.PinSystemBarIcons
import com.duylt.trave.vietlensai.core.designsystem.component.SovereigntySeal
import com.duylt.trave.vietlensai.core.designsystem.component.lacquerHatch
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.core.designsystem.theme.screenInsetsPadding

/**
 * The sovereignty statement in full — reached from the banner at the foot of the
 * passport, from explore, and from Settings.
 *
 * A whole screen of fixed lacquer red rather than a card on the app's own surface.
 * Every other page here is a place to do something; this one is the app saying
 * something, and it is the one screen that should look identical on every phone,
 * whatever the theme or the wallpaper says.
 *
 * The order is deliberate: the claim, then the map that shows what it is about, then
 * the evidence behind it. Prose first would leave the traveller reading about two
 * archipelagos they cannot place.
 */
@Composable
fun SovereigntyRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SovereigntyViewModel = koinViewModel(),
) {
    val map by viewModel.map.collectAsStateWithLifecycle()

    // Deep red under the bars in either theme, so the icons have to be light in both.
    PinSystemBarIcons(darkIcons = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Vermilion)
            .lacquerHatch()
            // Ahead of the scroll, so it insets the viewport rather than the content:
            // the red and its hatching still run under the status bar, but the
            // headline stops at it instead of sliding under the clock — or, with the
            // bars hidden, under the notch.
            .screenInsetsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter),
    ) {
        CloseChip(
            onClick = onBack,
            contentDescription = stringResource(Res.string.sovereignty_close),
            containerColor = PaperCream.copy(alpha = CHIP_ALPHA),
            contentColor = PaperCream,
        )

        Spacer(Modifier.height(24.dp))
        CompassMark()

        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(Res.string.sovereignty_page_title),
            style = MaterialTheme.typography.headlineMedium,
            color = PaperCream,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.sovereignty_page_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = Marigold,
        )

        Spacer(Modifier.height(20.dp))
        MapCard(map)

        Spacer(Modifier.height(22.dp))
        Statement()

        Spacer(Modifier.height(24.dp))
        WhereItAppears()

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PaperCream,
                contentColor = Vermilion,
            ),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            Text(
                text = stringResource(Res.string.sovereignty_understood),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * A compass rose reduced to its north point: the mark on an old chart that says
 * which way the map beneath it is oriented.
 *
 * Not the paper seal used further down the page. That one is a stamp of ownership
 * and belongs beside the sentence about where the statement appears; this one opens
 * a map, which is what follows it.
 */
@Composable
private fun CompassMark() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(PaperCream.copy(alpha = CHIP_ALPHA)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, size.height / 2f)
            val ring = 13.dp.toPx()
            drawLine(
                color = Marigold,
                start = Offset(centre.x, centre.y - 26.dp.toPx()),
                end = Offset(centre.x, centre.y - ring),
                strokeWidth = 1.5.dp.toPx(),
            )
            drawCircle(
                color = Marigold,
                radius = ring,
                center = centre,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            drawCircle(color = Marigold, radius = 3.dp.toPx(), center = centre)
        }
    }
}

/**
 * The map in a panel of its own.
 *
 * The panel keeps its height while the asset is being read, so the prose below does
 * not shuffle down the moment it arrives.
 */
@Composable
private fun MapCard(map: RegionMap?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(map?.aspectRatio ?: FRAME_ASPECT)
            .clip(RoundedCornerShape(20.dp))
            .background(SeaWash)
            .lacquerHatch(alpha = MAP_HATCH_ALPHA),
    ) {
        if (map != null) {
            SovereigntyMapCanvas(
                map = map,
                countryLabel = stringResource(Res.string.sovereignty_map_country),
                seaLabel = stringResource(Res.string.sovereignty_map_sea),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Statement() {
    val body = stringResource(Res.string.sovereignty_body)
    val source = stringResource(Res.string.sovereignty_body_source)

    // The document is named inside the sentence rather than pulled out into a
    // citation line, so it italicises in place — and falls back to plain text if a
    // translation words the sentence so that the title does not appear verbatim.
    val statement = remember(body, source) {
        buildAnnotatedString {
            append(body)
            val start = body.indexOf(source)
            if (start >= 0) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, start + source.length)
            }
        }
    }

    Text(
        text = statement,
        style = MaterialTheme.typography.bodyLarge,
        color = PaperCream,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(Res.string.sovereignty_body_secondary),
        style = MaterialTheme.typography.bodyLarge,
        color = PaperCream.copy(alpha = SECONDARY_ALPHA),
    )
}

/** Where the statement is shown, stamped rather than stated. */
@Composable
private fun WhereItAppears() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SeaWash)
            .lacquerHatch(alpha = MAP_HATCH_ALPHA)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SovereigntySeal(size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(Res.string.sovereignty_note),
            style = MaterialTheme.typography.bodyMedium,
            color = PaperCream.copy(alpha = SECONDARY_ALPHA),
        )
    }
}

/**
 * The panels: the page's own lacquer, darkened.
 *
 * Composited to an opaque colour rather than left as translucent black. Laid over
 * the page as a wash, the hatching underneath shows through and crosses each panel's
 * own hatching at a different phase, which reads as a printing fault rather than as
 * texture.
 */
private val SeaWash = Color.Black.copy(alpha = 0.18f).compositeOver(Vermilion)

/** Matches the frame in `sovereignty_map.json`; only used before it has been read. */
private const val FRAME_ASPECT = 1.56f

private const val CHIP_ALPHA = 0.14f
private const val MAP_HATCH_ALPHA = 0.04f
private const val SECONDARY_ALPHA = 0.72f
