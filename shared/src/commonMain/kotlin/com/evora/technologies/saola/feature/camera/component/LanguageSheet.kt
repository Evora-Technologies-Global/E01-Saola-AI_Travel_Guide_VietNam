package com.evora.technologies.saola.feature.camera.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.domain.model.TranslateLanguage
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.action_close
import com.evora.technologies.saola.resources.camera_translate_auto_detect
import com.evora.technologies.saola.resources.language_en
import com.evora.technologies.saola.resources.language_es
import com.evora.technologies.saola.resources.language_fr
import com.evora.technologies.saola.resources.language_ja
import com.evora.technologies.saola.resources.language_ko
import com.evora.technologies.saola.resources.language_th
import com.evora.technologies.saola.resources.language_vi
import com.evora.technologies.saola.resources.language_zh
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The language list, as a sheet.
 *
 * Choosing dismisses it rather than waiting for a confirm button: there is one
 * decision here, and a picker that needs a second tap to agree with the first is
 * asking the traveller to say it twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSheet(
    title: String,
    selected: TranslateLanguage?,
    unavailable: TranslateLanguage?,
    autoAllowed: Boolean,
    onSelect: (TranslateLanguage?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    // Slid away rather than switched off: leaving the composition unhides whatever
    // is behind the sheet in the same frame, which reads as the screen flickering.
    val close: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(
                start = ScreenGutter,
                end = Spacing.sm,
                bottom = Spacing.sm,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = close) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(Res.string.action_close),
                )
            }
        }

        Column(
            // Scrollable because the list outgrows a half-height sheet on a short
            // phone, and a language that cannot be reached cannot be chosen.
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = Spacing.lg),
        ) {
            if (autoAllowed) {
                LanguageSheetRow(
                    flag = null,
                    name = stringResource(Res.string.camera_translate_auto_detect),
                    gloss = null,
                    selected = selected == null,
                    enabled = true,
                    onClick = {
                        onSelect(null)
                        close()
                    },
                )
            }
            TranslateLanguage.entries.forEach { language ->
                val localised = stringResource(language.nameRes())
                LanguageSheetRow(
                    flag = language.flag,
                    name = language.displayName,
                    // Only where the two differ: "Tiếng Việt (Tiếng Việt)" tells
                    // a Vietnamese reader nothing they cannot already see.
                    gloss = localised.takeIf { it != language.displayName },
                    selected = language == selected,
                    // Greyed in place rather than dropped from the list: a list
                    // whose length changes with the other picker is one the
                    // traveller has to re-read every time they open it.
                    enabled = language != unavailable,
                    onClick = {
                        onSelect(language)
                        close()
                    },
                )
            }
        }
    }
}

@Composable
private fun LanguageSheetRow(
    flag: String?,
    name: String,
    gloss: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.xxs)
            // Faded whole rather than per-element: the flag is a colour emoji and
            // ignores content colour, so dimming the text alone left a bright flag
            // beside a greyed-out name.
            .alpha(if (enabled) 1f else 0.38f)
            .clip(MaterialTheme.shapes.medium)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (flag == null) {
                Icon(imageVector = Icons.Filled.Language, contentDescription = null)
            } else {
                Text(text = flag, style = MaterialTheme.typography.titleLarge)
            }
        }
        Spacer(Modifier.width(Spacing.md))
        Text(
            text = name,
            // The selected row already carries `surfaceVariant` behind it.
            style = MaterialTheme.typography.bodyLarge,
        )
        if (gloss == null) {
            Spacer(Modifier.weight(1f))
        } else {
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = "($gloss)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                // Takes the whole remainder so the tick stays pinned to the edge
                // whether or not the row carries a gloss.
                modifier = Modifier.weight(1f),
            )
        }
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** The language's name in the app's language, used as a gloss beside its own. */
private fun TranslateLanguage.nameRes(): StringResource = when (this) {
    TranslateLanguage.VIETNAMESE -> Res.string.language_vi
    TranslateLanguage.ENGLISH -> Res.string.language_en
    TranslateLanguage.JAPANESE -> Res.string.language_ja
    TranslateLanguage.KOREAN -> Res.string.language_ko
    TranslateLanguage.CHINESE -> Res.string.language_zh
    TranslateLanguage.FRENCH -> Res.string.language_fr
    TranslateLanguage.SPANISH -> Res.string.language_es
    TranslateLanguage.THAI -> Res.string.language_th
}
