package com.evora.technologies.saola.feature.journal.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.EmptyState
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.journal_empty_action
import com.evora.technologies.saola.resources.journal_empty_body
import com.evora.technologies.saola.resources.journal_empty_title
import com.evora.technologies.saola.resources.journal_favorites_empty_body
import com.evora.technologies.saola.resources.journal_favorites_empty_title
import com.evora.technologies.saola.resources.location_permission_blocked_body
import com.evora.technologies.saola.resources.location_permission_body
import com.evora.technologies.saola.resources.location_permission_grant
import com.evora.technologies.saola.resources.location_permission_title
import com.evora.technologies.saola.resources.permission_open_settings
import org.jetbrains.compose.resources.stringResource

/**
 * The three things the journal shows instead of days, in one file because they are one
 * question asked three ways: why is there nothing here?
 *
 * Because nothing has been photographed yet, because the filter is hiding it, or because the
 * app was never told where the traveller is. Each answer needs a different sentence and a
 * different way out, and keeping them together is what stops the next one being written as a
 * fourth empty state that looks nothing like the other three.
 */

/** No captures at all — the journal before the trip starts. */
@Composable
internal fun EmptyJournal(onOpenLens: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        title = stringResource(Res.string.journal_empty_title),
        description = stringResource(Res.string.journal_empty_body),
        modifier = modifier,
        action = {
            Button(onClick = onOpenLens) {
                Text(stringResource(Res.string.journal_empty_action))
            }
        },
    )
}

/**
 * The filter emptied a journal that is not itself empty.
 *
 * No action, deliberately: the chips stay above it, so the way back is already on screen and
 * a second one would read as a second thing to do.
 */
@Composable
internal fun EmptyFavorites(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.FavoriteBorder,
        title = stringResource(Res.string.journal_favorites_empty_title),
        description = stringResource(Res.string.journal_favorites_empty_body),
        modifier = modifier.padding(top = Spacing.xxl, bottom = Spacing.xl),
    )
}

/**
 * The whole screen, standing in for a journal that cannot be stamped yet.
 *
 * It leads with what the traveller gets — a province filling in on their passport — rather
 * than with what the app wants, because the system dialog that follows says "allow access to
 * this device's location" and nothing about why. Once Android stops asking, the same block
 * turns into the way to app settings: the only door left.
 *
 * @param isBlocked Android has stopped asking, so the button goes to system settings instead.
 *   Taken as a boolean rather than as the `LocationPermissionState` itself, for the reason
 *   `ExploreRoute` flattens the same object: a permission handle holds a launcher and a
 *   mutable flag, the Compose report marks it unstable, and a composable taking one can never
 *   skip. Two values and a lambda are what this block actually reads.
 */
@Composable
internal fun LocationPermissionPrompt(
    isBlocked: Boolean,
    onGrant: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        icon = Icons.Outlined.Place,
        title = stringResource(Res.string.location_permission_title),
        description = stringResource(
            if (isBlocked) {
                Res.string.location_permission_blocked_body
            } else {
                Res.string.location_permission_body
            },
        ),
        modifier = modifier,
        action = {
            Button(onClick = onGrant) {
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
