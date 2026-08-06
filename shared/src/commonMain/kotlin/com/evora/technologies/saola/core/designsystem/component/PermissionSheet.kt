package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion

/**
 * The app's one way of asking for a system permission in its own words first.
 *
 * Asking before the OS dialog is what buys the traveller a reason: the system prompt says
 * only what is being requested, never why, and a "no" to it is expensive — after the second
 * refusal Android stops showing the dialog at all. So the sheet explains, and only a "yes"
 * here spends the ask.
 *
 * Past that point [deniedForGood] flips both the explanation and the confirming button:
 * there is nothing left to ask, and a button that still said "Allow" would do nothing
 * visible when tapped. It sends them to the app's settings page instead.
 *
 * The sheet cannot be dragged or dismissed by tapping away. The two answers are the two
 * ways out, deliberately — a permission prompt that can be swiped into oblivion leaves the
 * caller unable to tell refusal from a stray gesture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSheet(
    title: String,
    body: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden },
    )
    ModalBottomSheet(
        onDismissRequest = {},
        sheetState = sheetState,
        // No drag handle: the sheet cannot be dragged away, and an affordance that
        // does nothing is worse than none at all.
        dragHandle = null,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenGutter)
                .padding(vertical = Spacing.xl),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.xl))
            // Both answers sit at the trailing end, the way a dialog's do: the way
            // out first, the one being asked for last and under the thumb.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
                ) {
                    Text(text = dismissLabel, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(Spacing.sm))
                Button(
                    onClick = onConfirm,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Vermilion,
                        contentColor = PaperCream,
                    ),
                    contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.sm),
                ) {
                    Text(text = confirmLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
