package com.evora.technologies.saola.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing

/**
 * The label that names a block within a page.
 *
 * There were three of these and only two were used. This one sat in the design system at
 * `titleMedium` on 20/12 padding with **zero** call sites; the settings page drew its own
 * at kicker weight on `ScreenGutter + 4` / 24 / 10, and the collection drew a third with a
 * count on the end at 20 / 10. Two private copies of a shared component, already a step
 * apart from each other — which is what a dead component in a design system always
 * produces, because the next person writes the one they need rather than fixing the one
 * that is wrong.
 *
 * The kicker style rather than a title scale: a section label names a group of rows, and
 * setting it at `titleMedium` puts it in competition with the row titles underneath it.
 *
 * @param trailing the count on the right, which only the collection has — "4/16" beside
 *   its category. A slot rather than a `String` so it can be a chip tomorrow.
 */
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ScreenGutter,
                end = ScreenGutter,
                top = PageSpacing.sectionGap,
                bottom = Spacing.sm,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Kicker(text = text, color = color)
        trailing?.invoke()
    }
}
