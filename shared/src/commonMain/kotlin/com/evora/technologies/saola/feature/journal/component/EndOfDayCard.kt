package com.evora.technologies.saola.feature.journal.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.core.designsystem.component.Kicker
import com.evora.technologies.saola.core.designsystem.theme.InkBrown
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Pill
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.journal_end_of_day_action
import com.evora.technologies.saola.resources.journal_end_of_day_body
import com.evora.technologies.saola.resources.journal_end_of_day_kicker
import com.evora.technologies.saola.resources.journal_end_of_day_title
import com.evora.technologies.saola.resources.journal_summary_generating
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The offer to turn today into a story.
 *
 * Fixed ink-and-marigold rather than scheme colours, like the lens controls: this is the one
 * block on a pale page that has to read as a different surface, and a tonal container drawn
 * from the same scheme as the cards around it does not.
 */
@Composable
internal fun EndOfDayCard(
    findCount: Int,
    isGenerating: Boolean,
    onWrite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenGutter, vertical = Spacing.md),
        shape = MaterialTheme.shapes.large,
        color = InkBrown,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Kicker(text = stringResource(Res.string.journal_end_of_day_kicker), color = Marigold)
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = pluralStringResource(
                    Res.plurals.journal_end_of_day_title,
                    findCount,
                    findCount,
                ),
                style = MaterialTheme.typography.headlineSmall,
                color = PaperCream,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(Res.string.journal_end_of_day_body),
                style = MaterialTheme.typography.bodyMedium,
                color = PaperCream.copy(alpha = BODY_ALPHA),
            )
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onWrite,
                enabled = !isGenerating,
                shape = Pill,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Vermilion,
                    contentColor = PaperCream,
                    disabledContainerColor = Vermilion.copy(alpha = DISABLED_CONTAINER_ALPHA),
                    disabledContentColor = PaperCream.copy(alpha = DISABLED_CONTENT_ALPHA),
                ),
                contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(SPINNER),
                        strokeWidth = SPINNER_STROKE,
                        color = PaperCream,
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(Res.string.journal_summary_generating),
                        style = MaterialTheme.typography.labelLarge,
                    )
                } else {
                    Text(
                        text = stringResource(Res.string.journal_end_of_day_action),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/** A spinner sized to sit inside a label line — a component's own geometry. */
private val SPINNER = 16.dp
private val SPINNER_STROKE = 2.dp

private const val BODY_ALPHA = 0.78f
private const val DISABLED_CONTAINER_ALPHA = 0.5f
private const val DISABLED_CONTENT_ALPHA = 0.7f
