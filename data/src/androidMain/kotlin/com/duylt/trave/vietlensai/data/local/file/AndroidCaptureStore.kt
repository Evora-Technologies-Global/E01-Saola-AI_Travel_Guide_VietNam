package com.duylt.trave.vietlensai.data.local.file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import android.content.Context
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.model.CaptureImage
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.util.AppError
import com.duylt.trave.vietlensai.domain.util.AppResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/** ML Kit needs real pixels, so this is also where the only `Bitmap` in `:data` lives. */
internal class AndroidCaptureStore(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher,
) : CaptureStore {

    private val imagesDir: File
        get() = File(context.filesDir, ImagePolicy.DIRECTORY).apply { if (!exists()) mkdirs() }

    override fun newCapturePath(): String =
        File(imagesDir, ImagePolicy.captureFileName(System.currentTimeMillis())).absolutePath

    override suspend fun read(path: String): AppResult<CaptureImage> =
        withContext(ioDispatcher) {
            val oriented = when (val read = readOriented(path)) {
                is AppResult.Failure -> return@withContext read
                is AppResult.Success -> read.data
            }
            try {
                val bytes = ByteArrayOutputStream().use { stream ->
                    oriented.compress(Bitmap.CompressFormat.JPEG, ImagePolicy.JPEG_QUALITY, stream)
                    stream.toByteArray()
                }
                AppResult.Success(
                    CaptureImage(bytes = bytes, widthPx = oriented.width, heightPx = oriented.height),
                )
            } catch (e: Exception) {
                log.e(e) { "Could not encode $path for upload" }
                AppResult.Failure(AppError.ImageUnavailable(e.message))
            } finally {
                oriented.recycle()
            }
        }

    /** Copies a picked gallery image into app storage so the journal keeps working after the picker's URI grant expires. */
    override suspend fun importFromPicker(source: String): AppResult<String> = withContext(ioDispatcher) {
        try {
            val target = File(newCapturePath())
            context.contentResolver.openInputStream(Uri.parse(source))?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return@withContext AppResult.Failure(
                AppError.ImageUnavailable("Could not open picked image"),
            )
            AppResult.Success(target.absolutePath)
        } catch (e: Exception) {
            log.e(e) { "Could not import picked image" }
            AppResult.Failure(AppError.ImageUnavailable(e.message))
        }
    }

    override suspend fun delete(path: String?) {
        withContext(ioDispatcher) {
            path?.let { runCatching { File(it).delete() } }
        }
    }

    /**
     * Re-encodes the file from the bytes [read] already corrected.
     *
     * Decoding through `readOriented` is what applies the EXIF rotation to the pixels; a
     * JPEG written back out of a `Bitmap` carries no orientation tag at all, so the upright
     * result cannot be un-done by a reader that ignores metadata.
     */
    override suspend fun flattenOrientation(path: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            val upright = when (val read = read(path)) {
                is AppResult.Failure -> return@withContext read
                is AppResult.Success -> read.data
            }
            try {
                File(path).writeBytes(upright.bytes)
                AppResult.Success(Unit)
            } catch (e: Exception) {
                log.e(e) { "Could not rewrite $path upright" }
                AppResult.Failure(AppError.ImageUnavailable(e.message))
            }
        }

    override suspend fun listCaptures(): List<String> = withContext(ioDispatcher) {
        runCatching { imagesDir.listFiles()?.map { it.absolutePath }.orEmpty() }
            .onFailure { log.w(it) { "Could not list the captures directory" } }
            .getOrDefault(emptyList())
    }

    /**
     * The capture as a bitmap the right way up, scaled down to a workable size.
     *
     * Internal to androidMain rather than part of the shared API: ML Kit needs the
     * pixels themselves, in the same orientation the traveller sees, because the boxes
     * it reports are only meaningful against the upright frame. The caller owns the
     * bitmap and must recycle it.
     */
    internal suspend fun readOriented(path: String): AppResult<Bitmap> =
        withContext(ioDispatcher) {
            val file = File(path)
            if (!file.exists()) {
                return@withContext AppResult.Failure(
                    AppError.ImageUnavailable("File not found: $path"),
                )
            }
            try {
                val decoded = decodeScaled(file)
                    ?: return@withContext AppResult.Failure(
                        AppError.ImageUnavailable("Could not decode image"),
                    )
                // Sampling alone lands anywhere between MAX_EDGE_PX and twice it, because
                // `inSampleSize` only honours powers of two. iOS resizes to exactly
                // MAX_EDGE_PX, so without this second step the same photograph was uploaded
                // at up to four times the pixels from Android — and held as four times the
                // bitmap while the camera was still bound.
                val clamped = clampToMaxEdge(decoded)
                if (clamped !== decoded) decoded.recycle()

                val oriented = applyExifRotation(file, clamped)
                if (oriented !== clamped) clamped.recycle()
                AppResult.Success(oriented)
            } catch (e: OutOfMemoryError) {
                log.e(e) { "Out of memory decoding $path" }
                AppResult.Failure(AppError.ImageUnavailable("Image too large"))
            } catch (e: Exception) {
                log.e(e) { "Could not read $path" }
                AppResult.Failure(AppError.ImageUnavailable(e.message))
            }
        }

    /** Two-pass decode: measure first, then load at the smallest sample size that still exceeds the target. */
    private fun decodeScaled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (longestEdge <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = ImagePolicy.sampleSizeFor(longestEdge)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    /**
     * Brings the sampled bitmap down to exactly [ImagePolicy.MAX_EDGE_PX] on its longest edge.
     *
     * Returns the input untouched when it is already small enough, so a picked library photo
     * of 800 px is never scaled up — that costs memory and invents no detail. The caller owns
     * both bitmaps and recycles the one it does not keep.
     */
    private fun clampToMaxEdge(bitmap: Bitmap): Bitmap {
        val longestEdge = maxOf(bitmap.width, bitmap.height)
        if (!ImagePolicy.needsExactResize(longestEdge)) return bitmap

        return Bitmap.createScaledBitmap(
            bitmap,
            ImagePolicy.clampedEdge(bitmap.width, longestEdge),
            ImagePolicy.clampedEdge(bitmap.height, longestEdge),
            // Filtered: a nearest-neighbour downscale of a photograph aliases text into
            // noise, and text is what the translate mode is pointed at.
            true,
        )
    }

    private fun applyExifRotation(file: File, bitmap: Bitmap): Bitmap {
        val degrees = runCatching {
            when (
                ExifInterface(file.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)

        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
