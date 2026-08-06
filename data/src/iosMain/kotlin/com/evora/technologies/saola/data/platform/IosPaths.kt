package com.evora.technologies.saola.data.platform

import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * The directory app-managed state is written to.
 *
 * Application Support rather than Documents on purpose: Documents is surfaced in the
 * Files app and backed up to iCloud, and a loose `saola.db` appearing there for
 * the traveller to move or delete is not something the app can recover from.
 * Captures, the database and the settings file all live here.
 *
 * Created on first use — iOS does not guarantee the directory exists.
 */
@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal fun iosAppSupportDirectory(): String {
    val manager = NSFileManager.defaultManager
    val base: NSURL = requireNotNull(
        manager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        ),
    ) { "Application Support directory is unavailable" }

    val path = requireNotNull(base.path) { "Application Support URL has no path" }
    if (!manager.fileExistsAtPath(path)) {
        manager.createDirectoryAtPath(
            path = path,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    return path
}

/** Where captured JPEGs go. A subdirectory so clearing history never walks siblings. */
internal fun iosCapturesDirectory(): String {
    val dir = "${iosAppSupportDirectory()}/captures"
    val manager = NSFileManager.defaultManager
    if (!manager.fileExistsAtPath(dir)) {
        manager.createDirectoryAtPath(
            path = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }
    return dir
}
