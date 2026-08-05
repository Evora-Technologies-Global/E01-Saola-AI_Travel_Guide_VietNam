package com.evora.technologies.saola.feature.sovereignty.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import com.evora.technologies.saola.core.designsystem.theme.Marigold
import com.evora.technologies.saola.core.designsystem.theme.PaperCream
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.sovereignty_body
import com.evora.technologies.saola.resources.sovereignty_body_secondary
import com.evora.technologies.saola.resources.sovereignty_body_source
import com.evora.technologies.saola.resources.sovereignty_page_subtitle
import com.evora.technologies.saola.resources.sovereignty_page_title
import com.evora.technologies.saola.resources.sovereignty_understood
import org.jetbrains.compose.resources.stringResource

// The statement's words — the heading, the prose, and the way out — in one file because they
// are one document read in one pass, and because a change to the voice of any of them that
// missed the others would be visible on a single screenful. The same test §5 sets for
// `SovereigntyPanel.kt` next door.
//
// **These used to be one composable.** `SovereigntyDocument` held the whole column and its KDoc
// argued that the statement is the one thing in this app that is identical on every device.
// That is still true of the *words*, and it stopped being true of the column on 04.08.2026,
// when the large window put the map beside the prose instead of between two halves of it. A
// column is an arrangement; §3 gives an arrangement to the branch that draws it. So the phone
// composes its order in `mobile/`, the large window its two panes in `tablet/`, and what they
// share is these three blocks and the map panel — no branch owns a copy of a word or a colour.
//
// Each block is a `Column` of its own rather than loose `Text`s emitted into the caller's, so
// neither branch can accidentally put a gap inside one: the gaps *within* a block belong to the
// block, the gaps *between* blocks belong to the arrangement.

/**
 * The mark, the claim, and the claim again in the other language.
 *
 * [CompassMark] rather than the paper seal: it opens a map, and a map is what the traveller is
 * about to be shown — on the phone directly below, on a large window in the pane alongside.
 */
@Composable
internal fun SovereigntyHeading(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        CompassMark()

        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(Res.string.sovereignty_page_title),
            style = MaterialTheme.typography.headlineMedium,
            color = PaperCream,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(Res.string.sovereignty_page_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = Marigold,
        )
    }
}

/** The statement in full: what is claimed, then what the app does about it. */
@Composable
internal fun SovereigntyStatement(modifier: Modifier = Modifier) {
    val body = stringResource(Res.string.sovereignty_body)
    val source = stringResource(Res.string.sovereignty_body_source)

    // The document is named inside the sentence rather than pulled out into a citation line,
    // so it italicises in place — and falls back to plain text if a translation words the
    // sentence so that the title does not appear verbatim.
    val statement = remember(body, source) {
        buildAnnotatedString {
            append(body)
            val start = body.indexOf(source)
            if (start >= 0) {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, start + source.length)
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = statement,
            style = MaterialTheme.typography.bodyLarge,
            color = PaperCream,
        )
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(Res.string.sovereignty_body_secondary),
            style = MaterialTheme.typography.bodyLarge,
            color = PaperCream.copy(alpha = SOVEREIGNTY_SECONDARY_ALPHA),
        )
    }
}

/**
 * The way out at the foot of the statement, in cream on the page's own red.
 *
 * **How wide it is, is the caller's.** The phone runs it the width of the page because there
 * the document *is* the page and the button is the last thing on it; a large window lets it
 * take the width of its own label, because a 440 dp button under a 440 dp column reads as a
 * second block of the document rather than as the end of it. The same division of labour
 * [SovereigntyCloseButton] makes for placement — and the reason neither branch holds a copy of
 * the colours, the shape or the label.
 */
@Composable
internal fun SovereigntyUnderstoodButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = PaperCream,
            contentColor = Vermilion,
        ),
        contentPadding = PaddingValues(horizontal = Spacing.xxl, vertical = Spacing.lg),
    ) {
        Text(
            text = stringResource(Res.string.sovereignty_understood),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
