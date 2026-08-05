package com.duylt.trave.vietlensai.feature.explore.component

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalPhone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.duylt.trave.vietlensai.core.designsystem.component.AccentChip
import com.duylt.trave.vietlensai.core.designsystem.component.AppAsyncImage
import com.duylt.trave.vietlensai.core.designsystem.component.Kicker
import com.duylt.trave.vietlensai.core.designsystem.theme.Jade
import com.duylt.trave.vietlensai.core.designsystem.theme.PageSpacing
import com.duylt.trave.vietlensai.core.designsystem.theme.PaperCream
import com.duylt.trave.vietlensai.core.designsystem.theme.ScreenGutter
import com.duylt.trave.vietlensai.core.designsystem.theme.Spacing
import com.duylt.trave.vietlensai.core.designsystem.theme.TempleGold
import com.duylt.trave.vietlensai.core.designsystem.theme.Vermilion
import com.duylt.trave.vietlensai.domain.model.NearbyPlace
import com.duylt.trave.vietlensai.domain.model.PlaceDetails
import com.duylt.trave.vietlensai.feature.explore.asCuisineLabel
import com.duylt.trave.vietlensai.feature.explore.asDistanceLabel
import com.duylt.trave.vietlensai.feature.explore.categoryIcon
import com.duylt.trave.vietlensai.feature.explore.compact
import com.duylt.trave.vietlensai.feature.explore.markerColor
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.explore_hours_title
import com.duylt.trave.vietlensai.resources.explore_read_more
import com.duylt.trave.vietlensai.resources.explore_readers
import com.duylt.trave.vietlensai.resources.explore_source_note
import com.duylt.trave.vietlensai.resources.explore_start_navigation
import org.jetbrains.compose.resources.stringResource

/**
 * Everything known about one place, in the order someone deciding whether to go reads it.
 *
 * The photograph and the name come first because they are what settles it, the
 * "Bắt đầu" button sits directly under them because that is the moment the decision is
 * made, and the article and opening hours follow because they are what a hesitant
 * reader scrolls for. Putting the button below the article would bury the one action on
 * the sheet under the longest thing on it.
 *
 * Everything above [details] comes from the search and is on screen the instant the
 * place opens. The rest arrives a moment later over the network, so nothing above it is
 * allowed to depend on it — this must be complete and useful before the second request
 * lands, and still complete if it never does.
 *
 * **The body, not the sheet.** The phone puts it inside a `ModalBottomSheet` and a large
 * window puts it in the column beside the map, which is the whole of the difference between
 * the two arrangements: on a phone the map is the screen, so reading about a place has to
 * cover it; on a tablet the column is already there and covering the map would be a choice.
 * The shell each branch wraps this in is that branch's business — this draws the place.
 *
 * **The column scrolls, in both of them.** Material3's `ModalBottomSheet` does not scroll its
 * content slot, and this is by far the tallest thing in the app to be put in one: without it
 * the opening hours and the article — the entire payload of the details request — sat below
 * the fold with no way to reach them. In a pane the same scroller is what keeps a long article
 * reachable in a column that cannot grow. The same fix the language sheet on the lens screen
 * already carries, for the same reason.
 */
@Composable
internal fun PlaceDetailBody(
    place: NearbyPlace,
    details: PlaceDetails?,
    isLoadingDetails: Boolean,
    onStartNavigation: () -> Unit,
    onOpenArticle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = markerColor(place.category)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = ScreenGutter)
            .padding(bottom = PageSpacing.listBottom),
    ) {
        if (place.photoUrl != null) {
            AppAsyncImage(
                model = place.photoUrl,
                contentDescription = place.name,
                modifier = Modifier.fillMaxWidth().height(PHOTO_HEIGHT),
                shape = MaterialTheme.shapes.medium,
                contentScale = ContentScale.Crop,
            )
        } else {
            // A banner in the place's own colour rather than a torn-photograph mark.
            // Most places have no picture, so this is the ordinary case.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BANNER_HEIGHT)
                    .clip(MaterialTheme.shapes.medium)
                    .background(accent.copy(alpha = CATEGORY_BANNER_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = categoryIcon(place.category),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(BANNER_GLYPH_SIDE),
                )
            }
        }
        Spacer(Modifier.height(Spacing.lg))

        place.typeLabel?.let { label ->
            Kicker(text = label, color = accent)
            Spacer(Modifier.height(Spacing.xs))
        }

        Text(
            text = place.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(Modifier.height(Spacing.sm))
        PlaceFacts(place = place, accent = accent)

        val summary = details?.fullSummary ?: place.summary
        if (summary != null) {
            Spacer(Modifier.height(Spacing.md))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = onStartNavigation,
            modifier = Modifier.fillMaxWidth(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Vermilion,
                contentColor = PaperCream,
            ),
            contentPadding = PaddingValues(vertical = Spacing.md),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(BUTTON_GLYPH_SIDE),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(Res.string.explore_start_navigation),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        ContactLines(place = place, accent = accent)

        // A spinner for the part that is still coming rather than for the whole place,
        // so what is already answerable stays readable while the rest arrives.
        if (isLoadingDetails) {
            Spacer(Modifier.height(Spacing.xl))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(SPINNER_SIDE), strokeWidth = 2.dp)
            }
        }

        details?.let { loaded ->
            PhotoStrip(photoUrls = loaded.photoUrls, placeName = place.name)
            loaded.articleUrl?.let { url ->
                Spacer(Modifier.height(Spacing.md))
                TextButton(onClick = { onOpenArticle(url) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(ARTICLE_GLYPH_SIDE),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(stringResource(Res.string.explore_read_more))
                }
            }
        }

        // Attribution, not decoration. OpenStreetMap's licence requires the source to be
        // credited wherever its data is shown, and Wikipedia's asks the same.
        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = stringResource(Res.string.explore_source_note),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

/**
 * The facts a decision turns on, on one line.
 *
 * There is no star rating here and there is not meant to be — see [NearbyPlace]. What
 * stands in its place is readership, which is a number that can be checked rather than
 * one this app made up.
 */
@Composable
private fun PlaceFacts(place: NearbyPlace, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AccentChip(text = place.distanceMeters.asDistanceLabel(), accent = accent)

        place.monthlyReaders?.takeIf { it > 0 }?.let { readers ->
            AccentChip(
                text = stringResource(Res.string.explore_readers, readers.compact()),
                accent = TempleGold,
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        tint = TempleGold,
                        modifier = Modifier.size(CHIP_GLYPH_SIDE),
                    )
                },
            )
        }

        place.cuisine?.let { cuisine ->
            AccentChip(text = cuisine.asCuisineLabel(), accent = Jade)
        }
    }
}

/** Opening hours, a website and a phone number, when whoever mapped the place added them. */
@Composable
private fun ContactLines(place: NearbyPlace, accent: Color) {
    val hours = place.openingHours
    val website = place.website
    val phone = place.phone
    if (hours == null && website == null && phone == null) return

    Spacer(Modifier.height(Spacing.xl))
    Kicker(
        text = stringResource(Res.string.explore_hours_title),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(Spacing.sm))

    hours?.let { IconLine(Icons.Outlined.Schedule, it, accent) }
    website?.let { IconLine(Icons.Outlined.Language, it, accent) }
    phone?.let { IconLine(Icons.Outlined.LocalPhone, it, accent) }
}

@Composable
private fun IconLine(icon: ImageVector, text: String, accent: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(top = Spacing.xxs).size(CONTACT_GLYPH_SIDE),
            tint = accent,
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PhotoStrip(photoUrls: List<String>, placeName: String) {
    if (photoUrls.isEmpty()) return

    Spacer(Modifier.height(Spacing.xl))
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        photoUrls.forEach { url ->
            AppAsyncImage(
                model = url,
                contentDescription = placeName,
                modifier = Modifier.size(width = THUMBNAIL_WIDTH, height = THUMBNAIL_HEIGHT),
                shape = MaterialTheme.shapes.small,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

/** Sizes, not gaps — none of these belongs on the `Spacing` scale. */
private val PHOTO_HEIGHT = 180.dp
private val BANNER_HEIGHT = 110.dp
private val BANNER_GLYPH_SIDE = 40.dp
private val BUTTON_GLYPH_SIDE = 20.dp
private val CHIP_GLYPH_SIDE = 14.dp
private val CONTACT_GLYPH_SIDE = 14.dp
private val ARTICLE_GLYPH_SIDE = 16.dp
private val SPINNER_SIDE = 24.dp
private val THUMBNAIL_WIDTH = 140.dp
private val THUMBNAIL_HEIGHT = 100.dp

/** Enough tint to read as a filled banner, little enough to keep the glyph legible. */
private const val CATEGORY_BANNER_ALPHA = 0.16f
