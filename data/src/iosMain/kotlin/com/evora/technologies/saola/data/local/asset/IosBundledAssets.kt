package com.evora.technologies.saola.data.local.asset

import com.evora.technologies.saola.data.platform.platformIoDispatcher
import com.evora.technologies.saola.data.util.log
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.withContext
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.stringWithContentsOfFile
import platform.posix.memcpy

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal class IosBundledAssets : BundledAssets {

    override suspend fun readText(name: String): String? = withContext(platformIoDispatcher()) {
        val path = resolve(name) ?: return@withContext null
        try {
            NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
        } catch (e: Exception) {
            log.e(e) { "Could not read bundled asset $name" }
            null
        }
    }

    /**
     * Always null today, and that is not a stub.
     *
     * The only caller is the demo seeder, whose assets are packaged by `:app`'s **debug**
     * variant on Android and are not in the iOS bundle at all. Adding them to the Xcode
     * target's Debug configuration is what would turn this on; until then an iOS debug build
     * opens on a real, empty journal. The code path is here rather than absent so that
     * enabling it stays a build-configuration change and not a Kotlin one.
     */
    override suspend fun readBytes(name: String): ByteArray? = withContext(platformIoDispatcher()) {
        val path = resolve(name, quiet = true) ?: return@withContext null
        try {
            val data: NSData = NSData.dataWithContentsOfFile(path) ?: return@withContext null
            val length = data.length.toInt()
            if (length == 0) return@withContext ByteArray(0)
            ByteArray(length).apply {
                usePinned { pinned -> memcpy(pinned.addressOf(0), data.bytes, data.length) }
            }
        } catch (e: Exception) {
            log.e(e) { "Could not read bundled asset $name" }
            null
        }
    }

    /**
     * Where [name] actually is inside the bundle.
     *
     * The Xcode target adds `iosApp/SharedAssets` as a folder reference, so the assets keep
     * that directory inside the bundle rather than landing at its root. The root is still
     * tried first, so a bundle that packages them flat also works. A name that carries its
     * own directory (`seed/demo-content.json`) is split, because `pathForResource` takes the
     * directory separately and will not find a slash in the resource name.
     */
    private fun resolve(name: String, quiet: Boolean = false): String? {
        val directory = name.substringBeforeLast('/', missingDelimiterValue = "")
        val fileName = name.substringAfterLast('/')
        val resourceName = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")

        val path = if (directory.isEmpty()) {
            NSBundle.mainBundle.pathForResource(resourceName, extension)
                ?: NSBundle.mainBundle.pathForResource(resourceName, extension, ASSET_DIRECTORY)
        } else {
            NSBundle.mainBundle.pathForResource(resourceName, extension, directory)
                ?: NSBundle.mainBundle.pathForResource(
                    resourceName,
                    extension,
                    "$ASSET_DIRECTORY/$directory",
                )
        }
        if (path == null && !quiet) log.e { "Bundled asset $name is not in the app bundle" }
        return path
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
