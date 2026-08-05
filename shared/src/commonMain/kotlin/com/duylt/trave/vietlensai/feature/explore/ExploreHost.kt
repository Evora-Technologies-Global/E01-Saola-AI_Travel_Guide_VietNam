package com.duylt.trave.vietlensai.feature.explore

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.component.showError
import com.duylt.trave.vietlensai.core.mvi.CollectEffects
import com.duylt.trave.vietlensai.core.util.rememberLocationPermissionState
import com.duylt.trave.vietlensai.core.util.userMessage
import com.duylt.trave.vietlensai.platform.rememberUrlOpener
import kotlinx.coroutines.launch

/**
 * Everything Explore does that is not layout, in one place both arrangements call.
 *
 * The second host in the app, and it exists for the reason the first one does — see
 * `feature/camera/LensHost.kt` and the exception `LLM.md` §5 makes for it. This Route is not
 * glue: it owns the permission bridge (the answer arrives in the composable, because it needs
 * an Activity result launcher on Android and a `CLLocationManager` on iOS, and the ViewModel
 * is told), the effect collection, and the one memoised intent dispatcher the map depends on
 * to skip. That is behaviour, and `LLM.md` §3 forbids a presentation branch from owning any.
 * Copied into the tablet screen, the next fix to any of the three would land on one form
 * factor and not the other.
 *
 * @param content the arrangement. It receives everything it needs to draw and one way to
 *   write back, and it places the composables — nothing else.
 */
@Composable
internal fun ExploreHost(
    viewModel: ExploreViewModel,
    content: @Composable (
        state: ExploreState,
        isDarkTheme: Boolean,
        isPermissionBlocked: Boolean,
        snackbarHostState: SnackbarHostState,
        onIntent: (ExploreIntent) -> Unit,
        onRequestPermission: () -> Unit,
        onOpenArticle: (String) -> Unit,
    ) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()
    val openUrl = rememberUrlOpener()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Remembered, where the other nine routes write `viewModel::onIntent` at the call
    // site. A bound callable reference captures its receiver, and the compose report
    // marks every ViewModel in this project `unstable` — so the compiler cannot memoise
    // it and the expression is a **new object on every recomposition**. Every child
    // taking it, and every lambda closing over it, is then unequal to its predecessor
    // and is denied the skip its `skippable` mark promises.
    //
    // On the other nine screens that costs a few wasted layout passes. Here the subtree
    // is a map: re-running `PlaceMap` re-enters the Maps SDK once per marker on Android
    // and re-runs the whole `UIKitView` update — the annotation diff, the selection
    // sweep — on iOS, and it did that on every emission, including the ones that only
    // moved a spinner.
    val onIntent = remember(viewModel) { viewModel::onIntent }

    // The permission state re-reads itself on resume, so this also covers the traveller
    // granting it out in system settings and coming back — the search starts by itself
    // rather than waiting for them to work out that they have to pull to refresh.
    LaunchedEffect(permission.isGranted) {
        onIntent(ExploreIntent.PermissionResolved(permission.isGranted))
    }

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is ExploreEffect.OpenUrl -> openUrl(effect.url)
            // From the effect's own payload. This effect is only raised when the map already
            // has markers, so there is no card on screen to read the failure off — which
            // makes it the one path where losing the message loses it entirely, and reading
            // `state.error` here lost it on every first failure. See `AppError.userMessage`.
            is ExploreEffect.ShowError -> scope.launch {
                snackbarHostState.showError(effect.error.userMessage())
            }
        }
    }

    content(
        state,
        MaterialTheme.colorScheme.surface.isDark(),
        permission.isDeniedForGood,
        snackbarHostState,
        onIntent,
        permission::requestOrOpenSettings,
        openUrl,
    )
}
