package com.evora.technologies.saola.feature.collection.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.collection_show_board
import com.evora.technologies.saola.resources.collection_show_guide
import org.jetbrains.compose.resources.stringResource

/**
 * The switch between the two ways of reading the collection.
 *
 * One button showing what it will do rather than what is on screen: in board mode it offers the
 * hints, in guide mode it offers the board back. A pair of segmented buttons would be more
 * literal and would cost the header twice the width for a choice with two options, on a page
 * whose title is already two lines in Vietnamese.
 *
 * It sits in `PageHeader`'s trailing slot on both arrangements, which is why it is here rather
 * than private to either: the phone's screen and the large window's pane both draw it, and a
 * second copy is a second icon.
 */
@Composable
internal fun CollectionViewToggle(
    isGuide: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalIconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (isGuide) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ListAlt,
            contentDescription = stringResource(
                if (isGuide) Res.string.collection_show_board else Res.string.collection_show_guide,
            ),
            modifier = Modifier.size(GLYPH),
        )
    }
}

/** Sized against the button's own 40 dp container, not against the spacing scale. */
private val GLYPH = 20.dp
