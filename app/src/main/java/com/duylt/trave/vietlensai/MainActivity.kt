package com.duylt.trave.vietlensai

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.duylt.trave.vietlensai.core.designsystem.theme.VietLensTheme
import com.duylt.trave.vietlensai.core.util.VolumeShutterBus
import com.duylt.trave.vietlensai.navigation.VietLensRoot
import com.duylt.trave.vietlensai.voice.TextToSpeechManager
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    /**
     * Held by the activity rather than a ViewModel so the engine starts warming up
     * as early as possible and is torn down deterministically with the window.
     */
    private val textToSpeech: TextToSpeechManager by inject()

    /** Routes the volume keys to the shutter, but only while a viewfinder wants them. */
    private val volumeShutter: VolumeShutterBus by inject()

    private val viewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold the splash until settings have loaded, so the app never paints in
        // the wrong theme and then snaps to the right one a frame later.
        splash.setKeepOnScreenCondition { !viewModel.isReady.value }

        // Both bars transparent and the page drawn under them. The status bar keeps no
        // scrim of its own, so what shows through it is whatever the screen painted up to
        // the top of the window — which is the point: the bar sits on the page rather than
        // in a band above it.
        enableEdgeToEdge()
        hideNavigationBar()
        textToSpeech.initialise()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            VietLensTheme(themePreference = settings.darkTheme) {
                // One call, and the fork is inside it: the phone shell and the large-window
                // shell are chosen by the window's own measurements, never by the host.
                VietLensRoot()
            }
        }
    }

    /**
     * Hides the navigation bar, and only that one.
     *
     * **The status bar stays.** The clock, the battery and the signal are what a traveller
     * checks without thinking — abroad, on a phone that has been out taking photographs all
     * day, more than usually — and the app has nothing to put in that strip that is worth
     * more than those three. It costs no layout either: it is transparent, the page runs
     * under it, and every screen already insets its *contents* with `screenInsetsPadding()`
     * while its background reaches the top of the window.
     *
     * The navigation bar is a different question and still goes: the app's own furniture —
     * the four tabs, the shutter row — already sits exactly where it would, and two rows of
     * chrome at the foot of a viewfinder is one too many. Transient rather than locked away:
     * a swipe from the bottom edge brings it back for a few seconds, so the back gesture is
     * never actually taken from the traveller.
     *
     * The icon colour is **not** set here. It follows the theme, which is not known until
     * settings have loaded, so `VietLensTheme` owns it through `DefaultSystemBarIcons` —
     * one answer for both platforms rather than a copy of the `when` in each host.
     */
    private fun hideNavigationBar() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }

    /**
     * Re-hides it on the way back in.
     *
     * The system shows the bar again whenever another window takes focus — the
     * permission dialog, the gallery picker, the recents switcher — and it stays up
     * until something asks for it to go.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideNavigationBar()
    }

    /**
     * Turns the volume keys into a shutter while the lens screen is open.
     *
     * Intercepted here rather than in Compose because key events reach the window,
     * not a focused composable — and both the down *and* the up are swallowed, or
     * the system volume panel slides in over the viewfinder anyway.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val isVolumeKey = event.keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (isVolumeKey && volumeShutter.isArmed) {
            // Repeats ignored: holding the button is one photo, not a burst.
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                volumeShutter.press()
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onDestroy() {
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
