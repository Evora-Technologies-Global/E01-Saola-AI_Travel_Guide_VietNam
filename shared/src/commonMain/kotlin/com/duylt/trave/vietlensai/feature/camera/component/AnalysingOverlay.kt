package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.analysing_history
import com.duylt.trave.vietlensai.resources.analysing_recognising
import com.duylt.trave.vietlensai.resources.analysing_stories
import com.duylt.trave.vietlensai.resources.analysing_title
import org.jetbrains.compose.resources.stringResource

/**
 * The recognition wait, over the frozen viewfinder.
 *
 * Recognition only — translate leaves this screen the moment the shutter closes,
 * so there is no translate copy here to choose between any more.
 */
@Composable
internal fun AnalysingOverlay(stage: Int, modifier: Modifier = Modifier) {
    // Dropped `analysing_nearby` along with the schema field it described: the
    // ticker reaches the last line inside the wait it is covering, so a line
    // promising work the request no longer asks for would be read as the reason
    // the traveller is still waiting.
    val stages = listOf(
        Res.string.analysing_recognising,
        Res.string.analysing_history,
        Res.string.analysing_stories,
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            // A scrim that reads as "hands off" has to behave like one. A Box with
            // only a background takes part in no pointer input at all, so taps carry
            // straight through it to the lens switch and the recent stack beneath.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(Res.string.analysing_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(stages[stage.coerceIn(0, stages.lastIndex)]),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = ScreenGutter),
            )
        }
    }
}
