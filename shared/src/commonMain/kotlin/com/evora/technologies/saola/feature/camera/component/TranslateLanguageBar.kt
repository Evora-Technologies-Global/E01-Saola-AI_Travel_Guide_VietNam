package com.evora.technologies.saola.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.feature.camera.LensIntent
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.camera_translate_auto
import com.evora.technologies.saola.resources.camera_translate_from
import com.evora.technologies.saola.resources.camera_translate_pick_from
import com.evora.technologies.saola.resources.camera_translate_pick_to
import com.evora.technologies.saola.resources.camera_translate_swap
import com.evora.technologies.saola.resources.camera_translate_to
import org.jetbrains.compose.resources.stringResource

/**
 * The "from → to" pair, on screen only while translate is the chosen mode.
 *
 * It sits at the foot of the frame rather than in the tool row because it belongs to
 * the picture, not to the camera: it is the last thing checked before pressing, and
 * the top of the frame is usually where the sign itself is. One capsule holds both
 * ends and the swap between them, so the pair reads as a single sentence instead of
 * as three unrelated controls.
 */
@Composable
internal fun TranslateLanguageBar(
    from: TranslateLanguage?,
    to: TranslateLanguage,
    onIntent: (LensIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Nothing to exchange until the source is a real language: swapping "detect it"
    // into the target would ask the translator to produce text in no language at all.
    val canSwap = from != null

    Row(
        modifier = modifier
            // The frame's width less a gutter, with equal halves either side of
            // the swap, so the button stays on the frame's axis. Sized to the two
            // names instead, it slid left and right as they changed length — and
            // the one control the traveller reaches for without looking was never
            // twice in the same place.
            .fillMaxWidth()
            .padding(horizontal = TRANSLATE_BAR_MARGIN)
            .clip(Pill)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LanguageChip(
            modifier = Modifier.weight(1f),
            selected = from,
            // The other end is struck off rather than left selectable: translating
            // English into English is not a request anyone means to make, and a
            // picker that silently reshuffled the far side to fix it would move a
            // choice the traveller had already made.
            unavailable = to,
            autoAllowed = true,
            description = stringResource(Res.string.camera_translate_from),
            sheetTitle = stringResource(Res.string.camera_translate_pick_from),
            onSelect = { onIntent(LensIntent.SelectTranslateFrom(it)) },
        )

        Box(
            modifier = Modifier
                .padding(horizontal = Spacing.xxs)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (canSwap) 0.16f else 0.06f))
                .clickable(
                    enabled = canSwap,
                    role = Role.Button,
                    onClick = { onIntent(LensIntent.SwapTranslateLanguages) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.SwapHoriz,
                contentDescription = stringResource(Res.string.camera_translate_swap),
                tint = Color.White.copy(alpha = if (canSwap) 1f else 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }

        LanguageChip(
            modifier = Modifier.weight(1f),
            selected = to,
            // Null while the source is being detected, which strikes nothing off:
            // any target is still a sensible answer until the sign is read.
            unavailable = from,
            autoAllowed = false,
            description = stringResource(Res.string.camera_translate_to),
            sheetTitle = stringResource(Res.string.camera_translate_pick_to),
            // Never null with the auto entry withheld, so the target keeps its
            // non-null type rather than being made nullable to suit the picker.
            onSelect = { language -> language?.let { onIntent(LensIntent.SelectTranslateTo(it)) } },
        )
    }
}

/**
 * One end of the translation, as a flag and a name that opens the picker.
 *
 * A sheet rather than a dropdown: the list is nine entries with a gloss on most of
 * them, which a menu anchored to a chip renders as a narrow column of clipped
 * names over the live preview. The sheet gets a title saying which end is being
 * chosen — the one thing a menu hanging off the chip never had room to say.
 */
@Composable
private fun LanguageChip(
    selected: TranslateLanguage?,
    /** The language the other end has taken; listed but not choosable. */
    unavailable: TranslateLanguage?,
    autoAllowed: Boolean,
    description: String,
    sheetTitle: String,
    onSelect: (TranslateLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picking by remember { mutableStateOf(false) }

    // Centred in the half the bar gives it: the chip is what moves when a name
    // changes length, and the swap button beside it does not.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Row(
            modifier = Modifier
                .clip(Pill)
                .clickable(role = Role.Button, onClickLabel = description) { picking = true }
                // Vertical pad up a step rather than down: this row opens the language
                // picker, and 4 dp either side would leave it a 28 dp target.
                .padding(start = Spacing.sm, end = Spacing.xxs, top = Spacing.sm, bottom = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected == null) {
                // A globe rather than a flag: "work it out" is not a country.
                Icon(
                    imageVector = Icons.Filled.Language,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(text = selected.flag, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(Spacing.xs))
            Text(
                text = selected?.displayName ?: stringResource(Res.string.camera_translate_auto),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Capped a little under the half the bar allots, so the longest
                // name still leaves the chevron beside it its full width.
                modifier = Modifier.widthIn(max = 80.dp),
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }

    if (picking) {
        LanguageSheet(
            title = sheetTitle,
            selected = selected,
            unavailable = unavailable,
            autoAllowed = autoAllowed,
            onSelect = onSelect,
            onDismiss = { picking = false },
        )
    }
}

/**
 * The gutter either side of the language bar, inside the frame.
 *
 * Wide enough that the bar reads as floating on the picture rather than bolted to
 * the frame: at a dozen dp it ran almost edge to edge while the dial above it sat
 * a hundred dp in from either side, and the two stopped looking like one stack.
 */
private val TRANSLATE_BAR_MARGIN = 24.dp
