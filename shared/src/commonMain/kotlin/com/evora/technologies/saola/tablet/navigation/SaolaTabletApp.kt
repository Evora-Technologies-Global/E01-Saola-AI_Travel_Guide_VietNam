package com.evora.technologies.saola.tablet.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.evora.technologies.saola.core.designsystem.component.SovereigntySeal
import com.evora.technologies.saola.core.designsystem.theme.PaneWidth
import com.evora.technologies.saola.core.designsystem.theme.ScreenInsets
import com.evora.technologies.saola.core.designsystem.theme.Vermilion
import com.evora.technologies.saola.navigation.Routes
import com.evora.technologies.saola.navigation.navigateToTopLevel
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.sovereignty_open
import org.jetbrains.compose.resources.stringResource

/**
 * The large-window shell: a rail down the left edge, everything else beside it.
 *
 * The rail replaces the bottom bar rather than moving it — on a 1194 dp window a bar across
 * the foot is a row of four icons with 900 dp of nothing between them, and the traveller's
 * hands are at the sides of the device, not under it. It hides on a detail screen for the
 * same reason the bar does: a discovery is a story, and navigation beside it invites people
 * to leave before they have read anything.
 *
 * [navController] has no default. It is created above the branch fork in
 * [com.evora.technologies.saola.navigation.SaolaRoot] and handed to whichever shell the
 * window size selects; a default here would let a caller create a second controller by
 * omission, and the traveller would lose their place every time the window changed size.
 */
@Composable
fun SaolaTabletApp(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    // `railDestination()` rather than `isTopLevel()`, and the difference is two routes: the
    // passport and the collection are detail screens on a phone and *are* the journal here.
    // See that function's KDoc for what judging this by the phone's answer costs.
    val current = currentDestination.railDestination()

    Surface(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = current != null,
                // Slide **and** size, unlike the phone's bar. There is no `Scaffold` reporting
                // a rail's live width the way `bottomBar` reports a bar's height, so a slide
                // on its own would leave the 104 dp slot standing until the animation ended
                // and then collapse it in one frame — the page beside it jumping sideways by
                // a rail's width. `shrinkHorizontally` clips rather than re-measures, so the
                // rail's own contents are laid out once and the labels do not reflow as it
                // goes.
                enter = slideInHorizontally { -it } +
                    expandHorizontally(expandFrom = Alignment.Start),
                exit = slideOutHorizontally { -it } +
                    shrinkHorizontally(shrinkTowards = Alignment.Start),
            ) {
                SaolaNavigationRail(
                    current = current,
                    onSelect = { destination -> navController.navigateToTopLevel(destination.route) },
                    onOpenSovereignty = { navController.navigate(Routes.SOVEREIGNTY) },
                )
            }

            SaolaTabletNavHost(navController = navController)
        }
    }
}

/**
 * The four places, plus the seal that is not one of them.
 *
 * `NavigationRail` and `NavigationRailItem` rather than a hand-built column of buttons: the
 * phone draws its four with `NavigationBarItem`, and the project's constraint is that the
 * tablet re-arranges the phone's components rather than re-drawing them. Same selection
 * indicator, same label placement, same accessibility semantics — the rail is the same list
 * turned on its side.
 *
 * It is [PaneWidth.rail] wide rather than the Material default of 80 dp, which is the one
 * measurement the wireframe fixes here. The extra 24 dp is what lets "Khám phá" and "Ống
 * kính" sit under their icons; a rail narrow enough to clip a label is a rail that reads as
 * broken in seven of the app's eight languages and fine in the one it was checked in.
 *
 * The seal sits below a `weight(1f)` spacer, pinned to the foot. It is **the second way in**
 * to the sovereignty statement, not a replacement: Settings keeps its own row, exactly as on
 * the phone. It is not a [RailDestination] because it does not switch place — making it one
 * would light it up as the selected tab and the rail's selection state would be a lie.
 */
@Composable
private fun SaolaNavigationRail(
    current: RailDestination?,
    onSelect: (RailDestination) -> Unit,
    onOpenSovereignty: () -> Unit,
) {
    NavigationRail(
        modifier = Modifier.width(PaneWidth.rail),
        // The status bar's inset and the notch's, unioned. Material's own default is the
        // system bars alone, which misses the notch — and on a tablet held in landscape that
        // hole is on a side edge, which here is the edge the rail stands on. Handed to
        // `NavigationRail` rather than wrapped around it so the inset lands inside its
        // surface: the rail's colour still reaches the glass, under the status bar, and only
        // the icons move in.
        windowInsets = ScreenInsets,
    ) {
        RailDestination.entries.forEach { destination ->
            val selected = destination == current
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = {
                    // Centred and left to wrap. A label that needs two lines gets two lines;
                    // ellipsis would turn "Ống kính" into "Ống…" and say nothing at all.
                    Text(text = stringResource(destination.labelRes), textAlign = TextAlign.Center)
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val openStatement = stringResource(Res.string.sovereignty_open)
        Box(
            modifier = Modifier
                .clip(CircleShape)
                // Marigold on vermilion, the same pairing `SovereigntyBanner` prints the seal
                // on. The stamp's rings and lettering are marigold, and on the rail's own
                // surface — cream in the light scheme — they would be a shape nobody can read.
                .background(Vermilion)
                .clickable(role = Role.Button, onClick = onOpenSovereignty)
                // On the seal itself rather than on the ring inside it: the whole disc is the
                // target, and a screen reader should hear what the button opens rather than
                // spell out the three words printed on the stamp.
                .semantics { contentDescription = openStatement },
        ) {
            SovereigntySeal()
        }
    }
}
