package com.duylt.trave.vietlensai.feature.sovereignty.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.duylt.trave.vietlensai.core.designsystem.component.OverlayIconButton
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.sovereignty_close
import org.jetbrains.compose.resources.stringResource

/**
 * The way out of the statement, in the page's own ink.
 *
 * [OverlayIconButton] is the app's one overlay affordance (`LLM.md` §13.5) and this is the
 * override its KDoc names: the statement is a solid sheet of lacquer red rather than a
 * photograph, so the default black glass would read as a hole punched in the page.
 *
 * A composable rather than two call sites passing the same two colours, because the two
 * branches disagree about *where* it goes and about nothing else. The phone keeps it at the
 * head of the scrolling column, where it travels up with the prose; a large window pins it to
 * the far corner of a page that is wider than the document on it. Placement is the modifier's
 * business, and it is all either branch decides here.
 */
@Composable
internal fun SovereigntyCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OverlayIconButton(
        icon = Icons.Filled.Close,
        contentDescription = stringResource(Res.string.sovereignty_close),
        onClick = onClick,
        containerColor = LacquerChip,
        contentColor = PaperCream,
        modifier = modifier,
    )
}
