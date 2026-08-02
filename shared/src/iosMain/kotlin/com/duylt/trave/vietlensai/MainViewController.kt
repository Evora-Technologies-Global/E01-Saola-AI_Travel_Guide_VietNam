package com.duylt.trave.vietlensai

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.duylt.trave.vietlensai.core.designsystem.theme.VietLensTheme
import com.duylt.trave.vietlensai.di.appModules
import com.duylt.trave.vietlensai.domain.model.AppSettings
import com.duylt.trave.vietlensai.navigation.VietLensApp
import com.duylt.trave.vietlensai.platform.IosStatusBarStyle
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIStatusBarStyle
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.UIViewController

/**
 * The two functions Swift calls.
 *
 * Everything above them — every screen, every ViewModel, the whole object graph — is the same
 * code the Android app runs. `iosApp` is a nine-line SwiftUI shell around
 * [VietLensViewController].
 */

/**
 * Starts the object graph. Call once, from the app delegate, before the first controller.
 *
 * Nothing is passed in: unlike Android, where `:data` needs the application `Context` to find
 * its files, the iOS side resolves the bundle and Application Support directory from process
 * globals — so the graph is entirely self-contained here.
 *
 * @param debug true for a development build; controls how much reaches the Xcode console. The
 *   app logs prompt metadata and model routing decisions, which should not be left on in a
 *   build handed to anyone else. `iOSApp.swift` passes it from `#if DEBUG`, which is the iOS
 *   counterpart of `BuildConfig.DEBUG` — the same flag `:app` hands to [appModules]. It also
 *   reaches the HTTP client, which only logs traffic when it is true.
 */
fun startVietLens(debug: Boolean) {
    Logger.setMinSeverity(if (debug) Severity.Verbose else Severity.Warn)
    startKoin { modules(appModules(debug)) }
}

/**
 * The app's root view controller.
 *
 * A subclass rather than a bare `ComposeUIViewController` because of the status bar: Android
 * lets a composable flip the bar icons through a window flag, while iOS asks the view
 * controller. [IosStatusBarStyle] is where a screen publishes what it wants, and this is what
 * answers with it.
 */
fun VietLensViewController(): UIViewController = VietLensRootViewController()

@OptIn(ExperimentalForeignApi::class)
private class VietLensRootViewController :
    UIViewController(nibName = null, bundle = null) {

    /**
     * The engine is warmed up with the window and torn down with it, matching what
     * `MainActivity` does on Android — text-to-speech init costs the better part of a second,
     * and paying that on the first tap of the speak button makes the feature feel broken.
     */
    private val textToSpeech: TextToSpeechManager by lazy {
        KoinPlatform.getKoin().get()
    }

    private val composeController = ComposeUIViewController {
        val viewModel: MainViewModel = KoinPlatform.getKoin().get()
        // Not `collectAsStateWithLifecycle`: on iOS the composition's lifecycle is the
        // controller's, and this controller is only ever the root one.
        val settings: AppSettings by viewModel.settings.collectAsState()
        VietLensTheme(themePreference = settings.darkTheme) {
            VietLensApp()
        }
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        textToSpeech.initialise()

        addChildViewController(composeController)
        view.addSubview(composeController.view)
        composeController.view.setFrame(view.bounds)
        composeController.didMoveToParentViewController(this)

        // Re-asks iOS for the bar style whenever a screen pushes or pops a request.
        IosStatusBarStyle.onChanged = { setNeedsStatusBarAppearanceUpdate() }
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        composeController.view.setFrame(view.bounds)
    }

    override fun preferredStatusBarStyle(): UIStatusBarStyle = IosStatusBarStyle.current

    /*
     * The Android build hides the system bars, because the whole screen is a viewfinder and the
     * app's own furniture already sits where they would be. There is no equivalent here:
     * `prefersHomeIndicatorAutoHidden` is exposed to Kotlin as a read-only extension property
     * rather than an overridable method, so the home indicator stays visible on iOS. Compose
     * draws under it either way — the lens controls use safe-area insets — so the difference is
     * a thin line over the bottom of the viewfinder, not a layout problem.
     */

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        textToSpeech.shutdown()
        IosStatusBarStyle.onChanged = null
    }
}
