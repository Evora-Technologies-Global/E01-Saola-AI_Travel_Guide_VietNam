package com.evora.technologies.saola.feature.camera.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.LensMode
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/**
 * Whether the framing tip has already had its turn this run.
 *
 * Process-scoped rather than held in the ViewModel: the tip answers "what do I
 * point this at", which is worth one appearance when the app opens and is noise
 * every time the traveller comes back from the journal — and a back-stack-scoped
 * ViewModel is recreated on exactly those returns.
 *
 * Process-scoped also means it survives the window changing size, which is the answer this
 * needs now that there are two arrangements: rotating an iPad swaps the phone's bubble for
 * the tablet's and back, and a tip that reappeared on each swap would be a tip the traveller
 * has to dismiss by waiting, repeatedly.
 */
private object CaptureHintSession {
    var shown = false
}

/**
 * The framing tip, shown once at the head of the frame and then gone.
 *
 * It used to sit permanently under the shutter, where a sentence that is only ever
 * read once cost a band of the screen for the whole session. As a bubble it lands
 * where the eye already is — on the picture — says its piece, and gives the frame
 * back. Above the subject rather than across it, so the corners keep saying where to
 * aim while it is read. Marked as shown the moment it starts so a recomposition
 * mid-fade cannot hand the traveller a second one.
 */
@Composable
internal fun CaptureHintBubble(mode: LensMode, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (CaptureHintSession.shown) return@LaunchedEffect
        CaptureHintSession.shown = true
        visible = true
        delay(CAPTURE_HINT_VISIBLE_MILLIS)
        visible = false
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(durationMillis = 220)),
        exit = fadeOut(tween(durationMillis = 320)),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(mode.hintRes()),
            style = MaterialTheme.typography.labelLarge,
            color = InkBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = ScreenGutter)
                .clip(Pill)
                .background(Marigold)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
}

/** How long the framing tip stays up before fading out, once per app run. */
private const val CAPTURE_HINT_VISIBLE_MILLIS = 1_500L
