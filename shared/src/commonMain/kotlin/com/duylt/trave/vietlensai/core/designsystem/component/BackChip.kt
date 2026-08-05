package com.duylt.trave.vietlensai.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.action_back
import org.jetbrains.compose.resources.stringResource

/**
 * The back affordance on pushed screens: a round chip rather than a bare arrow.
 *
 * Screens like the passport put their title in the page itself instead of in a
 * `TopAppBar`, and an unbounded arrow floating beside a headline reads as part of
 * the heading. The chip gives it its own footprint and a 40 dp target.
 */
@Composable
fun BackChip(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    IconChip(
        onClick = onClick,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        contentDescription = stringResource(Res.string.action_back),
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
    )
}

@Composable
private fun IconChip(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}
