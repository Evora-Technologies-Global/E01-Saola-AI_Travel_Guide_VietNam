package com.evora.technologies.saola.data.local.file

import com.evora.technologies.saola.data.platform.iosCapturesDirectory
import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.model.CaptureImage
import com.evora.technologies.saola.domain.repository.CaptureStore
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.roundToInt
import kotlin.time.Clock

/**
 * The iOS half of [ImageStorage].
 *
 * `UIImage` does the EXIF work that `ExifInterface` does on Android — it carries the
 * capture's `imageOrientation`, and redrawing it into a graphics context bakes that
 * rotation into the pixels. That is the whole reason the downscale below goes through
 * `draw(in:)` rather than a straight `CGImage` resize: it corrects orientation and
 * resamples in one pass, so the bytes handed to Gemini and to Vision are upright.
 */
@OptIn(ExperimentalForeignApi::class)
internal class IosCaptureStore(
    private val ioDispatcher: CoroutineDispatcher,
) : CaptureStore {

    override fun newCapturePath(): String =
        "${iosCapturesDirectory()}/" +
            ImagePolicy.captureFileName(Clock.System.now().toEpochMilliseconds())

    override fun nameOf(nameOrPath: String): String = ImagePolicy.fileNameOf(nameOrPath)

    /**
     * Rebuilt from the container's location *now*, never from a path stored earlier.
     *
     * This is the whole fix: `iosCapturesDirectory()` asks the file manager where
     * Application Support is on this launch, and that answer changes under the app when
     * iOS re-homes the container after an install, an update or a restore.
     */
    override fun resolve(nameOrPath: String): String =
        "${iosCapturesDirectory()}/${nameOf(nameOrPath)}"

    override suspend fun read(path: String): AppResult<CaptureImage> =
        withContext(ioDispatcher) {
            val resolved = resolve(path)
            if (!NSFileManager.defaultManager.fileExistsAtPath(resolved)) {
                return@withContext AppResult.Failure(
                    AppError.ImageUnavailable("File not found: $resolved"),
                )
            }
            try {
                val source = UIImage.imageWithContentsOfFile(resolved)
                    ?: return@withContext AppResult.Failure(
                        AppError.ImageUnavailable("Could not decode image"),
                    )
                val oriented = source.downscaledUpright()
                    ?: return@withContext AppResult.Failure(
                        AppError.ImageUnavailable("Could not redraw image"),
                    )
                val jpeg = UIImageJPEGRepresentation(
                    oriented,
                    ImagePolicy.JPEG_QUALITY / 100.0,
                ) ?: return@withContext AppResult.Failure(
                    AppError.ImageUnavailable("Could not encode JPEG"),
                )

                val size = oriented.size
                AppResult.Success(
                    CaptureImage(
                        bytes = jpeg.toByteArray(),
                        // `size` is in points; the context below is created at scale 1,
                        // so points and pixels are the same number here. Rounding
                        // rather than truncating keeps a 1023.6 px edge at 1024.
                        widthPx = size.useContents { width }.roundToInt(),
                        heightPx = size.useContents { height }.roundToInt(),
                    ),
                )
            } catch (e: Exception) {
                log.e(e) { "Could not read $resolved" }
                AppResult.Failure(AppError.ImageUnavailable(e.message))
            }
        }

    /**
     * Copies a picked image into app storage.
     *
     * The picker on iOS hands back a file URL the app can read directly, so unlike
     * Android there is no content resolver to go through — but the copy still has to
     * happen, because the URL points into a temporary container the system reclaims.
     */
    override suspend fun importFromPicker(source: String): AppResult<String> = withContext(ioDispatcher) {
        try {
            val sourcePath = source.removePrefix("file://")
            val data = NSData.dataWithContentsOfFile(sourcePath)
                ?: return@withContext AppResult.Failure(
                    AppError.ImageUnavailable("Could not open picked image"),
                )
            val target = newCapturePath()
            if (!data.writeToFile(target, atomically = true)) {
                return@withContext AppResult.Failure(
                    AppError.ImageUnavailable("Could not write picked image"),
                )
            }
            AppResult.Success(target)
        } catch (e: Exception) {
            log.e(e) { "Could not import picked image" }
            AppResult.Failure(AppError.ImageUnavailable(e.message))
        }
    }

    override suspend fun delete(path: String?) {
        withContext(ioDispatcher) {
            path?.let {
                runCatching { NSFileManager.defaultManager.removeItemAtPath(resolve(it), null) }
            }
        }
    }

    /**
     * Re-encodes the file from the bytes [read] already corrected.
     *
     * `downscaledUpright` redraws through a graphics context, which is what bakes
     * `imageOrientation` into the pixels; the JPEG written back out carries no orientation
     * of its own, so no reader can rotate it a second time.
     */
    override suspend fun flattenOrientation(path: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            val upright = when (val read = read(path)) {
                is AppResult.Failure -> return@withContext read
                is AppResult.Success -> read.data
            }
            try {
                if (!upright.bytes.toNSData().writeToFile(resolve(path), atomically = true)) {
                    return@withContext AppResult.Failure(
                        AppError.ImageUnavailable("Could not rewrite $path upright"),
                    )
                }
                AppResult.Success(Unit)
            } catch (e: Exception) {
                log.e(e) { "Could not rewrite $path upright" }
                AppResult.Failure(AppError.ImageUnavailable(e.message))
            }
        }

    override suspend fun listCaptures(): List<String> = withContext(ioDispatcher) {
        runCatching {
            // `contentsOfDirectoryAtPath` already returns bare names, which is exactly
            // what the sweep compares against the database.
            NSFileManager.defaultManager
                .contentsOfDirectoryAtPath(iosCapturesDirectory(), null)
                .orEmpty()
                .filterIsInstance<String>()
        }
            .onFailure { log.w(it) { "Could not list the captures directory" } }
            .getOrDefault(emptyList())
    }

    /**
     * Redraws the image upright and no larger than [ImagePolicy.MAX_EDGE_PX].
     *
     * Scale is forced to 1 so the resulting pixel dimensions equal the point
     * dimensions — on a 3× device the default would silently produce a bitmap three
     * times the requested edge, undoing the downscale this exists for.
     */
    private fun UIImage.downscaledUpright(): UIImage? {
        val (width, height) = size.useContents { width to height }
        if (width <= 0.0 || height <= 0.0) return null

        val longestEdge = maxOf(width, height)
        val ratio = if (longestEdge > ImagePolicy.MAX_EDGE_PX) {
            ImagePolicy.MAX_EDGE_PX / longestEdge
        } else {
            1.0
        }
        val targetSize = CGSizeMake(width * ratio, height * ratio)

        UIGraphicsBeginImageContextWithOptions(targetSize, false, 1.0)
        try {
            // drawInRect honours imageOrientation, which is what bakes the EXIF
            // rotation into the output pixels.
            drawInRect(CGRectMake(x = 0.0, y = 0.0, width = width * ratio, height = height * ratio))
            return UIGraphicsGetImageFromCurrentImageContext()
        } finally {
            UIGraphicsEndImageContext()
        }
    }
}

/** Copies an `NSData` into a Kotlin `ByteArray`. */
@OptIn(ExperimentalForeignApi::class)
internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    return ByteArray(size).apply {
        usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    }
}

/** Wraps a Kotlin `ByteArray` in an `NSData` without an extra copy on the Kotlin side. */
@OptIn(ExperimentalForeignApi::class)
internal fun ByteArray.toNSData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toNSData), length = size.toULong())
}
