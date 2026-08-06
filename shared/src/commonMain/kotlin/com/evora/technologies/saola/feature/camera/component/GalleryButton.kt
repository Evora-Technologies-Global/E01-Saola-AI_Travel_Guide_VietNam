package com.evora.technologies.saola.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_pick_gallery
import org.jetbrains.compose.resources.stringResource

/**
 * The camera roll, as a film-canister slot.
 *
 * A bare icon: it sits opposite a pile of photographs that also opens something, and
 * the two are told apart by their shapes — this one goes out to the phone's own
 * library, the other into the journal.
 *
 * It is deliberately never gated on the camera grant. The picker asks the system for one
 * photo and hands back a copy, which needs no permission of its own on either platform — so
 * a traveller who refused the camera, or has not been asked yet, can still give the app a
 * photograph they already have.
 *
 * The slot carries its own ink rather than letting the backdrop through, for the same reason
 * the drum draws a dark ring inside its rim: this control does not sit on one known surface.
 * On the phone it is over a live preview that can be a white wall; on the tablet it is on the
 * panel beside the viewfinder, which in the light scheme is cream — and a cream wash under a
 * cream icon on cream is the button disappearing entirely. Ink under marigold reads on both.
 */
@Composable
internal fun GalleryButton(enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .size(GALLERY_SLOT_SIZE)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(InkBrown)
            .border(width = 1.dp, color = Marigold.copy(alpha = 0.55f), shape = shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.PhotoLibrary,
            contentDescription = stringResource(Res.string.camera_pick_gallery),
            tint = PaperCream,
            modifier = Modifier.size(24.dp),
        )
    }
}

private val GALLERY_SLOT_SIZE = 52.dp
