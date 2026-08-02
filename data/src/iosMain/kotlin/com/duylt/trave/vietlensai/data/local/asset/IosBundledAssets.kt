package com.duylt.trave.vietlensai.data.local.asset

import com.duylt.trave.vietlensai.data.platform.platformIoDispatcher
import com.duylt.trave.vietlensai.data.util.log
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal class IosBundledAssets : BundledAssets {

    override suspend fun readText(name: String): String? = withContext(platformIoDispatcher()) {
        try {
            // The Xcode target adds `iosApp/SharedAssets` as a folder reference, so the assets
            // keep that directory inside the bundle rather than landing at its root. The root
            // is still tried first, so a bundle that packages them flat also works.
            val resourceName = name.substringBeforeLast('.')
            val extension = name.substringAfterLast('.', missingDelimiterValue = "")
            val path = NSBundle.mainBundle.pathForResource(resourceName, extension)
                ?: NSBundle.mainBundle.pathForResource(resourceName, extension, ASSET_DIRECTORY)
                ?: run {
                    log.e { "Bundled asset $name is not in the app bundle" }
                    return@withContext null
                }
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        } catch (e: Exception) {
            log.e(e) { "Could not read bundled asset $name" }
            null
        }
    }
}

/**
 * Matches the folder reference the Xcode target copies; see `copySharedAssetsToIos`.
 *
 * Deliberately not "Resources": a directory of that name at the root of an iOS `.app` makes
 * CFBundle look for `Resources/Info.plist`, and the app then fails to install with
 * "Missing bundle ID".
 */
private const val ASSET_DIRECTORY = "SharedAssets"
