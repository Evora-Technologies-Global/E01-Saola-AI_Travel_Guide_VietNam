package com.evora.technologies.saola

import android.app.Application
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import com.evora.technologies.saola.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SaolaApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Release builds log nothing below a warning: the app logs prompt metadata and model
        // routing decisions, which should not reach a shipped device's logcat.
        Logger.setMinSeverity(if (BuildConfig.DEBUG) Severity.Verbose else Severity.Warn)

        startKoin {
            if (BuildConfig.DEBUG) androidLogger(Level.ERROR)
            // The one thing the shared graph cannot produce for itself: `:data` resolves the
            // database path, the asset manager and the location provider through this context.
            androidContext(this@SaolaApplication)
            // This is the only place in the Android build that knows which build type is
            // running: `:shared` and `:data` are single-variant multiplatform modules, so the
            // same class files are linked into the debug and the release APK. Handing the flag
            // over here is what keeps `HttpClient` from installing Ktor's logger in release.
            modules(appModules(BuildConfig.DEBUG))
        }
    }
}
