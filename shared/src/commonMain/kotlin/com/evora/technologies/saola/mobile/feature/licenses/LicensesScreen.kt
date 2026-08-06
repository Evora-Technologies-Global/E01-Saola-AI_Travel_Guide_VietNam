package com.evora.technologies.saola.mobile.feature.licenses

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.evora.technologies.saola.core.designsystem.component.PageHeader
import com.evora.technologies.saola.core.designsystem.theme.PageSpacing
import com.evora.technologies.saola.core.designsystem.theme.ScreenGutter
import com.evora.technologies.saola.core.designsystem.theme.Spacing
import com.evora.technologies.saola.core.designsystem.theme.screenInsetsPadding
import com.evora.technologies.saola.platform.rememberUrlOpener
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.licenses_commons_body
import com.evora.technologies.saola.resources.licenses_commons_title
import com.evora.technologies.saola.resources.licenses_footer
import com.evora.technologies.saola.resources.licenses_intro
import com.evora.technologies.saola.resources.licenses_kicker
import com.evora.technologies.saola.resources.licenses_natural_earth_body
import com.evora.technologies.saola.resources.licenses_natural_earth_title
import com.evora.technologies.saola.resources.licenses_osm_body
import com.evora.technologies.saola.resources.licenses_osm_title
import com.evora.technologies.saola.resources.licenses_title
import com.evora.technologies.saola.resources.licenses_wikipedia_body
import com.evora.technologies.saola.resources.licenses_wikipedia_title
import org.jetbrains.compose.resources.stringResource

/**
 * Where everything on screen came from, and under what terms.
 *
 * ODbL §4.3 asks for a notice "reasonably calculated" to reach whoever sees the result, and
 * `MapSourceNote` is that notice on the two maps. This page is the long form it points at: the
 * licence by name, which part of the app rests on it, and a way through to the licence itself.
 * It also covers the three sources that have no map to sit on — Natural Earth's coastline,
 * Wikipedia's prose and Commons' photographs — each of which asks for credit too, and none of
 * which had any anywhere in the app before this screen existed.
 *
 * **The one screen in the app with no ViewModel, and the argument is that there is nothing for
 * one to hold.** No state, nothing asynchronous, nothing that can fail: five paragraphs fixed
 * at build time and four links. `MviViewModel` would add a Koin binding, a suite, and an effect
 * channel with no sender, and the reducer would be the identity function. That is the same
 * shape of exemption `MainViewModel` carries in `LLM.md` §3 — stated in writing rather than
 * left to be filed as an oversight — and it stops here: a screen that acquires a decision
 * acquires a ViewModel in the same change.
 *
 * **No large-window arrangement, deliberately, and both shells register this same route.** It
 * is a scrolling document, which is the same picture at any width — the reason
 * `TranslationRoute` is likewise the phone's screen in `TabletNavGraph`. Registering it in both
 * graphs is not optional whatever it draws: `setGraph` compares the two structurally, and a
 * route only one shell declares clears the back stack on every resize.
 */
@Composable
fun LicensesRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openUrl = rememberUrlOpener()

    LicensesScreen(onBack = onBack, onOpenUrl = openUrl, modifier = modifier)
}

@Composable
private fun LicensesScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .screenInsetsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        PageHeader(
            title = stringResource(Res.string.licenses_title),
            kicker = stringResource(Res.string.licenses_kicker),
            onBack = onBack,
        )

        Text(
            text = stringResource(Res.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = ScreenGutter),
        )

        // OpenStreetMap first because it is the one with a share-alike obligation attached
        // rather than a courtesy, and the only one two screens of this app are built on.
        Spacer(Modifier.height(PageSpacing.sectionGap))
        LicenseCard(
            title = stringResource(Res.string.licenses_osm_title),
            body = stringResource(Res.string.licenses_osm_body),
            onOpen = { onOpenUrl(OSM_COPYRIGHT_URL) },
        )

        Spacer(Modifier.height(Spacing.md))
        LicenseCard(
            title = stringResource(Res.string.licenses_natural_earth_title),
            body = stringResource(Res.string.licenses_natural_earth_body),
            onOpen = { onOpenUrl(NATURAL_EARTH_TERMS_URL) },
        )

        Spacer(Modifier.height(Spacing.md))
        LicenseCard(
            title = stringResource(Res.string.licenses_wikipedia_title),
            body = stringResource(Res.string.licenses_wikipedia_body),
            onOpen = { onOpenUrl(CC_BY_SA_URL) },
        )

        Spacer(Modifier.height(Spacing.md))
        LicenseCard(
            title = stringResource(Res.string.licenses_commons_title),
            body = stringResource(Res.string.licenses_commons_body),
            onOpen = { onOpenUrl(COMMONS_LICENSING_URL) },
        )

        // Last, and about the one body of content on the page that is not borrowed: the
        // traveller's own photographs. A page listing what the app owes other people is the
        // honest place to say what it does with what is theirs.
        Spacer(Modifier.height(PageSpacing.sectionGap))
        Text(
            text = stringResource(Res.string.licenses_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenGutter),
        )

        Spacer(Modifier.height(PageSpacing.listBottom))
    }
}

/** The page the licence itself lives on, for each source, in the order the cards appear. */
private const val OSM_COPYRIGHT_URL = "https://www.openstreetmap.org/copyright"
private const val NATURAL_EARTH_TERMS_URL = "https://www.naturalearthdata.com/about/terms-of-use/"
private const val CC_BY_SA_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
private const val COMMONS_LICENSING_URL = "https://commons.wikimedia.org/wiki/Commons:Licensing"
