package com.evora.technologies.saola.core.designsystem.component

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
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing

/**
 * The top of a document screen: the journal, the settings, the collection, the passport
 * and the chat. One component, so those five stop being five.
 *
 * They used to each build their own, and the boxes did not agree on anything. Top and
 * bottom padding ran 0/16, 0/4, 12/4, 12/4 and 10/12; the title was drawn at
 * `headlineLarge`, `headlineMedium`, `headlineMedium`, `headlineMedium` and `titleLarge`
 * — five titles at four sizes. Nothing on any one screen looked wrong. The app looked
 * wrong, because the traveller crosses three of these in about four seconds and the
 * heading moved every time.
 *
 * **The screen supplies strings, not styles.** There is no `titleStyle` parameter and
 * there will not be one: the moment a screen can pass its own, this component is a layout
 * helper rather than a standard, and the five headers grow back.
 *
 * ## What this deliberately does not do
 *
 * **It does not apply the top inset.** `screenInsetsPadding()` stays on the screen's
 * outermost container, which is what `Insets.kt` argues for at length — in landscape the
 * display cutout moves to one side, and the whole page has to move out from under it, not
 * just its heading. A header that took the inset would leave every screen's *body* running
 * under the notch in landscape. [OverlayHeader] does apply it, and the difference between
 * the two is the whole reason they are separate components: an overlay header floats over
 * content instead of sitting in a column above it.
 *
 * @param kicker the eyebrow above the title. Drawn in [Kicker]'s stamped style, in the
 *   primary colour, because on every screen that has one it names the section the page
 *   belongs to rather than the page.
 * @param onBack renders a [BackChip] to the left of the title when present. A screen
 *   reached from a tab passes null; a screen pushed onto one passes its pop lambda.
 * @param trailing the count, the clear button, whatever the page keeps at the far end of
 *   its heading row. A slot rather than a typed action list so it can hold anything and
 *   stay skippable.
 * @param colors see [PageHeaderColors]. The one axis a screen may override, and only
 *   because two screens in this app draw on a fixed palette by design.
 */
@Composable
fun PageHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    colors: PageHeaderColors = PageHeaderDefaults.colors(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = ScreenGutter,
                end = ScreenGutter,
                top = PageSpacing.headerTop,
                bottom = PageSpacing.headerToContent,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            BackChip(
                onClick = onBack,
                containerColor = colors.backContainer,
                contentColor = colors.backContent,
            )
            Spacer(Modifier.width(Spacing.md))
        }

        Column(modifier = Modifier.weight(1f)) {
            if (kicker != null) {
                Kicker(text = kicker, color = colors.kicker)
                Spacer(Modifier.height(Spacing.xs))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.title,
                // Two lines, because a chat's title is the discovery's name and comes
                // from the model rather than from a string resource.
                maxLines = TITLE_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.subtitle,
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/**
 * The colours a [PageHeader] draws in.
 *
 * The one thing a screen may override, and it exists for exactly one reason: the chat
 * and the sovereignty statement are fixed to the lacquer palette on purpose rather than
 * taken from the colour scheme, argued in `Color.kt` and protected by `LLM.md` §12. A
 * fixed-palette screen either gets to pass its colours here or goes back to hand-rolling
 * its own header, and a hand-rolled header is the thing this component exists to end.
 *
 * There is deliberately no equivalent for *type*. Colour is where these screens genuinely
 * differ; a header drawn at a different size is just a header that has drifted.
 */
@Immutable
data class PageHeaderColors(
    val title: Color,
    val subtitle: Color,
    val kicker: Color,
    val backContainer: Color,
    val backContent: Color,
)

object PageHeaderDefaults {

    /** Scheme colours — what nine of the ten screens use and what nothing should override. */
    @Composable
    fun colors(
        title: Color = MaterialTheme.colorScheme.onSurface,
        subtitle: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        kicker: Color = MaterialTheme.colorScheme.primary,
        backContainer: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        backContent: Color = MaterialTheme.colorScheme.onSurface,
    ) = PageHeaderColors(
        title = title,
        subtitle = subtitle,
        kicker = kicker,
        backContainer = backContainer,
        backContent = backContent,
    )
}

private const val TITLE_MAX_LINES = 2
