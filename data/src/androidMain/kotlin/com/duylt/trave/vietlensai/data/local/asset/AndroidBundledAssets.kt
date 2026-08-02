package com.duylt.trave.vietlensai.data.local.asset

import android.content.Context
import com.duylt.trave.vietlensai.data.platform.platformIoDispatcher
import com.duylt.trave.vietlensai.data.util.log
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
}
