package com.evora.technologies.saola.feature.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.EmptyState
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_retry
import com.evora.technologies.saola.resources.explore_empty_body
import com.evora.technologies.saola.resources.explore_loading
import com.evora.technologies.saola.resources.explore_permission_blocked_body
import com.evora.technologies.saola.resources.explore_permission_body
import com.evora.technologies.saola.resources.explore_permission_title
import com.evora.technologies.saola.resources.location_permission_grant
import com.evora.technologies.saola.resources.nav_explore
import com.evora.technologies.saola.resources.permission_open_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The three states that stand over a map rather than beside it.
 *
 * One file because they are one decision seen three times — an opaque, touch-swallowing sheet
 * laid over a map engine that is already warming up underneath — and [mapCover] is the
 * decision itself. Split across three files, the next state added would get two of its three
 * properties and nobody would notice which one was missing.
 */

/**
 * The ground a whole-screen state stands on, over a map that is already running.
 *
 * Two jobs, and the screen is wrong without either. **Opaque**, because the map is composed
 * *underneath* these rather than instead of them, and a transparent state would show a map of
 * Hanoi warming up behind the sentence explaining that we do not know where the traveller is.
 * And **it swallows touches**: an empty `pointerInput` is hit-tested like any other pointer
 * node, so the drag that would otherwise reach straight through to the map — panning a map
 * nobody can see, under a spinner — stops here instead.
 *
 * Anything else a branch floats over the map has to carry the same guard. The large window's
 * results column is the second thing that does: without it, scrolling the list would pan the
 * map underneath it.
 */
@Composable
internal fun Modifier.mapCover(): Modifier = this
    .fillMaxSize()
    .background(MaterialTheme.colorScheme.background)
    .pointerInput(Unit) {}
    .screenInsetsPadding()

@Composable
internal fun MapLoadingCover() {
    Column(
        modifier = Modifier.mapCover(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIDE), strokeWidth = 3.dp)
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(Res.string.explore_loading),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

/**
 * No location yet, and the two quite different reasons for that.
 *
 * Past a permanent denial the OS will not show its dialog again, so both the explanation and
 * the button have to change: a button still labelled "Allow" would do nothing visible when
 * tapped. The same rule `PermissionSheet` follows, applied to a whole-screen state — this
 * screen has nothing at all to show without a fix, so a sheet over an empty map would be a
 * prompt floating on nothing.
 */
@Composable
internal fun MapPermissionCover(isBlocked: Boolean, onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.mapCover()) {
        EmptyState(
            icon = Icons.Outlined.LocationOff,
            title = stringResource(Res.string.explore_permission_title),
            description = stringResource(
                if (isBlocked) {
                    Res.string.explore_permission_blocked_body
                } else {
                    Res.string.explore_permission_body
                },
            ),
            action = {
                FilledTonalButton(onClick = onRequestPermission) {
                    Text(
                        stringResource(
                            if (isBlocked) {
                                Res.string.permission_open_settings
                            } else {
                                Res.string.location_permission_grant
                            },
                        ),
                    )
                }
            },
        )
    }
}

@Composable
internal fun MapFailureCover(message: String?, onRetry: () -> Unit) {
    Column(modifier = Modifier.mapCover()) {
        EmptyState(
            icon = Icons.Outlined.Explore,
            title = stringResource(Res.string.nav_explore),
            description = message ?: stringResource(Res.string.explore_empty_body),
            action = {
                FilledTonalButton(onClick = onRetry) {
                    Text(stringResource(Res.string.action_retry))
                }
            },
        )
    }
}

/** A size, not a gap — the spinner is the one thing on an otherwise empty screen. */
private val SPINNER_SIDE = 40.dp
