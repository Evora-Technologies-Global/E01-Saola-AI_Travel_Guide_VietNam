package com.duylt.trave.vietlensai.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.theme.InkBrown
import com.duylt.trave.vietlensai.core.designsystem.theme.Marigold
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.StampType
import com.duylt.trave.vietlensai.feature.camera.CameraCapabilities
import com.duylt.trave.vietlensai.feature.camera.CaptureTimer
import com.duylt.trave.vietlensai.feature.camera.LensFacing
import com.duylt.trave.vietlensai.feature.camera.LensIntent
import com.duylt.trave.vietlensai.feature.camera.LensState
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.camera_flash_off
import com.duylt.trave.vietlensai.resources.camera_flash_on
import com.duylt.trave.vietlensai.resources.camera_grid_hide
import com.duylt.trave.vietlensai.resources.camera_grid_show
import com.duylt.trave.vietlensai.resources.camera_switch_lens
import com.duylt.trave.vietlensai.resources.camera_timer_10
import com.duylt.trave.vietlensai.resources.camera_timer_3
import com.duylt.trave.vietlensai.resources.camera_timer_off
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Flash, grid, lens switch and self-timer, spread across one row above the frame.
 *
 * A row rather than the rail that used to run down the side of the preview: these
 * four are the phone's settings rather than the picture's, so they belong off the
 * image entirely — and a rail over the frame covered the one thing this screen is
 * for. Controls the hardware cannot honour are absent rather than greyed out; a
 * phone with one camera should not be told it is missing something.
 *
 * @param horizontalArrangement the one thing the two branches genuinely disagree about. The
 *   phone gives the row the full width and spreads the four evenly across it; the tablet has
 *   the mode chips sharing that row, so the tools gather at its end. Everything else about
 *   them — size, colour, which ones appear — is the same on both.
 */
@Composable
internal fun CameraToolRow(
    state: LensState,
    capabilities: CameraCapabilities,
    onIntent: (LensIntent) -> Unit,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceEvenly,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (capabilities.hasFlash) {
            ToolButton(
                icon = if (state.flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                description = stringResource(
                    if (state.flashEnabled) Res.string.camera_flash_off else Res.string.camera_flash_on,
                ),
                active = state.flashEnabled,
                role = Role.Switch,
                onClick = { onIntent(LensIntent.ToggleFlash) },
            )
        }

        ToolButton(
            icon = Icons.Filled.Grid3x3,
            description = stringResource(
                if (state.gridEnabled) Res.string.camera_grid_hide else Res.string.camera_grid_show,
            ),
            active = state.gridEnabled,
            role = Role.Switch,
            onClick = { onIntent(LensIntent.ToggleGrid) },
        )

        if (capabilities.hasFrontCamera) {
            ToolButton(
                icon = Icons.Filled.Cameraswitch,
                description = stringResource(Res.string.camera_switch_lens),
                active = state.lensFacing == LensFacing.FRONT,
                onClick = { onIntent(LensIntent.SwitchLens) },
            )
        }

        ToolButton(
            icon = if (state.timer == CaptureTimer.OFF) Icons.Filled.TimerOff else Icons.Filled.Timer,
            description = stringResource(state.timer.labelRes()),
            active = state.timer != CaptureTimer.OFF,
            badge = if (state.timer == CaptureTimer.OFF) null else "${state.timer.seconds}",
            onClick = { onIntent(LensIntent.CycleTimer) },
        )
    }
}

/**
 * One round camera control.
 *
 * Active is marigold-on-ink rather than a subtle tint: whether the flash is armed is
 * the kind of thing a traveller checks at a glance in a dark temple, and a state
 * that has to be looked for twice is a state that gets missed.
 */
@Composable
private fun ToolButton(
    icon: ImageVector,
    description: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: Role = Role.Button,
    badge: String? = null,
) {
    val content = if (active) InkBrown else PaperCream
    Box(
        modifier = modifier
            .size(TOOL_BUTTON_SIZE)
            .clip(CircleShape)
            .background(if (active) Marigold else PaperCream.copy(alpha = 0.12f))
            .clickable(role = role, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = content,
            modifier = Modifier.size(20.dp),
        )
        if (badge != null) {
            Text(
                text = badge,
                style = StampType.ordinal,
                color = content,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = Spacing.xs, bottom = Spacing.xxs),
            )
        }
    }
}

private fun CaptureTimer.labelRes(): StringResource = when (this) {
    CaptureTimer.OFF -> Res.string.camera_timer_off
    CaptureTimer.THREE -> Res.string.camera_timer_3
    CaptureTimer.TEN -> Res.string.camera_timer_10
}

private val TOOL_BUTTON_SIZE = 44.dp
