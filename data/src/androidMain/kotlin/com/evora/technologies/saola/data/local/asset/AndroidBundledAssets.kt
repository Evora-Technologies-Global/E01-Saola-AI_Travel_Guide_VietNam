package com.evora.technologies.saola.data.local.asset

import android.content.Context
import com.evora.technologies.saola.data.platform.platformIoDispatcher
import com.evora.technologies.saola.data.util.log
import kotlinx.coroutines.withContext

internal class AndroidBundledAssets(private val context: Context) : BundledAssets {

    override suspend fun readText(name: String): String? = withContext(platformIoDispatcher()) {
        try {
            context.assets.open(name).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            log.e(e) { "Could not read bundled asset $name" }
            null
        }
    }

    override suspend fun readBytes(name: String): ByteArray? = withContext(platformIoDispatcher()) {
        try {
            context.assets.open(name).use { it.readBytes() }
        } catch (e: Exception) {
            // Debug logged rather than error: the seed assets are packaged by `:app`'s debug
            // variant only, so a release build reaching here is the design working, not a fault.
            log.d { "Bundled asset $name is not packaged in this build" }
            null
        }
    }
}
