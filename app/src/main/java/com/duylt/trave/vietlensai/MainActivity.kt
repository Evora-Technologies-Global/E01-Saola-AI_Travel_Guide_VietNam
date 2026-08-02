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
import com.duylt.trave.vietlensai.navigation.VietLensApp
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

        enableEdgeToEdge()
        hideSystemBars()
        textToSpeech.initialise()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            VietLensTheme(themePreference = settings.darkTheme) {
                VietLensApp()
            }
        }
    }

    /**
     * Hides the status and navigation bars for the whole app.
     *
     * The screen is the viewfinder and the page both, and the app's own furniture —
     * the lens controls, the four tabs — already sits where the system bars would.
     * Two rows of chrome saying the same thing is one too many.
     *
     * Transient rather than locked away: a swipe from either edge brings the bars
     * back over the content for a few seconds, so the clock and the back gesture are
     * never actually taken from the traveller.
     */
    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    /**
     * Re-hides them on the way back in.
     *
     * The system shows the bars again whenever another window takes focus — the
     * permission dialog, the gallery picker, the recents switcher — and they stay up
     * until something asks for them to go.
     */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
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
