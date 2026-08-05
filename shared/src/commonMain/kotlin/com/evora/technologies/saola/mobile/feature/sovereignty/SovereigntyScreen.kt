package com.evora.technologies.saola.mobile.feature.sovereignty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.evora.technologies.saola.core.designsystem.component.PinSystemBarIcons
import com.evora.technologies.saola.core.designsystem.component.lacquerHatch
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.feature.sovereignty.RegionMap
import com.evora.technologies.saola.feature.sovereignty.SovereigntyViewModel
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyCloseButton
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyHeading
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyMapPanel
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyNote
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyStatement
import com.evora.technologies.saola.feature.sovereignty.component.SovereigntyUnderstoodButton
import com.evora.technologies.saola.feature.sovereignty.component.frameAspect
import org.koin.compose.viewmodel.koinViewModel

/**
 * The sovereignty statement in full — reached from the banner at the foot of the passport,
 * from explore, and from Settings.
 *
 * A whole screen of fixed lacquer red rather than a card on the app's own surface. Every other
 * page here is a place to do something; this one is the app saying something, and it is the
 * one screen that should look identical on every phone, whatever the theme or the wallpaper
 * says.
 *
 * The words and the two panels are shared with the large-window arrangement; what is in this
 * file is the phone's vessel for them, and its order. The sheet of red, one scroll down the
 * whole page, and the close chip at the head of that scroll rather than pinned over it — on a
 * screen this narrow the document *is* the page, so a button floating over it would sit on top
 * of the prose it is meant to leave.
 *
 * **The order is the argument.** The claim, then the map that shows what it is about, then the
 * evidence behind it, then where the statement appears. Prose first would leave the traveller
 * reading about two archipelagos they cannot place — which is why on a phone the map interrupts
 * the sentence rather than following it, and why a large window, which can show both at once,
 * is free to arrange them differently.
 */
@Composable
fun SovereigntyRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SovereigntyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SovereigntyScreen(map = state.map, onBack = onBack, modifier = modifier)
}

@Composable
private fun SovereigntyScreen(
    map: RegionMap?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Deep red under the bars in either theme, so the icons have to be light in both.
    PinSystemBarIcons(darkIcons = false)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Vermilion)
            .lacquerHatch()
            // Ahead of the scroll, so it insets the viewport rather than the content: the red
            // and its hatching still run under the status bar, but the headline stops at it
            // instead of sliding under the clock — or, held sideways, under the notch.
            .screenInsetsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter),
    ) {
        SovereigntyCloseButton(onClick = onBack)

        Spacer(Modifier.height(Spacing.xl))
        SovereigntyHeading()

        Spacer(Modifier.height(Spacing.xl))
        SovereigntyMapPanel(
            map = map,
            modifier = Modifier.fillMaxWidth().aspectRatio(map.frameAspect),
        )

        Spacer(Modifier.height(Spacing.xl))
        SovereigntyStatement()

        Spacer(Modifier.height(Spacing.xl))
        SovereigntyNote()

        Spacer(Modifier.height(Spacing.xl))
        SovereigntyUnderstoodButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.xxl))
    }
}
