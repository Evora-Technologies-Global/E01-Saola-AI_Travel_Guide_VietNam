package com.duylt.trave.vietlensai.feature.passport.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.domain.model.Discovery
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.passport_sheet_open_discovery
import org.jetbrains.compose.resources.stringResource

/**
 * The photographs behind the tally.
 *
 * This strip is the whole reason the province panel exists in its current form. It used to end
 * on the sentence "4 discoveries here", which stated a number the traveller could not open —
 * the one screen in the app that knew where their photos were and would not show them.
 *
 * Its height is fixed rather than measured from the tiles. The strip straddles the peek fold,
 * so it is on screen before its query has returned: left to wrap, it would be nothing at all
 * for the first frames after a province is tapped, and the panel would visibly grow under the
 * traveller's eyes as the photographs landed.
 */
@Composable
internal fun DiscoveryStrip(
    discoveries: List<Discovery>,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth().height(StripHeight),
        contentPadding = PaddingValues(horizontal = ScreenGutter),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        items(discoveries, key = { it.id }) { discovery ->
            DiscoveryTile(discovery = discovery, onClick = { onOpen(discovery.id) })
        }
    }
}

@Composable
private fun DiscoveryTile(discovery: Discovery, onClick: () -> Unit) {
    Column(modifier = Modifier.width(TileWidth)) {
        AppAsyncImage(
            model = discovery.imagePath,
            contentDescription = stringResource(
                Res.string.passport_sheet_open_discovery,
                discovery.title,
            ),
            shape = MaterialTheme.shapes.medium,
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = discovery.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val TileWidth = 104.dp

/** A square tile, the gap, and two lines of caption — held whether or not both are used. */
private val StripHeight = 147.dp
