package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.PassportStamp
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_discovery_count
import com.duylt.trave.vietlensai.resources.passport_sheet_expand
import com.duylt.trave.vietlensai.resources.passport_sheet_no_stamp
import com.duylt.trave.vietlensai.resources.passport_sheet_unlock_order
import com.duylt.trave.vietlensai.resources.passport_status_locked
import com.duylt.trave.vietlensai.resources.passport_status_unlocked
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The band the peek height stops at: the stamp, the name, and the tally.
 *
 * Fixed in height whether the province is stamped or not. A locked province used to produce a
 * much shorter panel than a stamped one, so dragging across the map made the sheet jump up and
 * down as much as it changed what it said.
 */
@Composable
internal fun ProvinceHead(
    stamp: PassportStamp,
    unlockOrder: Int?,
    accent: Color,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        // Tappable as well as draggable. Material gives the drag handle expand and
        // collapse actions for accessibility services but no pointer click at all, so
        // until this the only way to open the panel with a finger was to drag a 32 dp
        // bar — discoverable if you already knew, invisible if you did not.
        //
        // The vertical padding is the room the tilt needs: rotation is a draw
        // transform and claims no layout, so without it the stamp's raised corner
        // would be cut off by whatever sits above.
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClickLabel = stringResource(Res.string.passport_sheet_expand),
                onClick = onExpand,
            )
            .padding(horizontal = ScreenGutter, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProvinceStamp(stamp = stamp, accent = accent)
        Spacer(Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Kicker(
                text = stringResource(
                    if (stamp.isUnlocked) Res.string.passport_status_unlocked
                    else Res.string.passport_status_locked,
                ),
                color = accent,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = stamp.province.displayName,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = if (stamp.isUnlocked) {
                    val tally = pluralStringResource(
                        Res.plurals.passport_discovery_count,
                        stamp.discoveryCount,
                        stamp.discoveryCount,
                    )
                    // The rank is only ever missing for a stamp whose discoveries all
                    // predate the passport and were backfilled without a timestamp.
                    unlockOrder
                        ?.let {
                            "$tally · ${
                                stringResource(
                                    Res.string.passport_sheet_unlock_order,
                                    it
                                )
                            }"
                        }
                        ?: tally
                } else {
                    stringResource(Res.string.passport_sheet_no_stamp)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
