package com.duylt.trave.vietlensai.domain.repository

import com.duylt.trave.vietlensai.domain.model.CaptureImage
import com.duylt.trave.vietlensai.domain.util.AppResult

/**
 * Where the photos the traveller takes live, and how they are read back.
 *
 * A port rather than a concrete store: writing a JPEG, correcting its EXIF rotation and
 * downscaling it are all device capabilities, so the domain states what it needs and each
 * platform decides how. Everything that comes out is already upright — an upside-down
 * photo of a pagoda measurably degrades recognition, and phones very often store portrait
 * shots as landscape-plus-a-rotation-flag.
 *
 * Declared here rather than in the data layer because three different callers need it: the
 * lens screen picks a path to shoot into, recognition reads the bytes to upload, and the
 * passport map reads them to clip into a province outline.
 */
interface CaptureStore {

    /** An absolute path for a capture that has not been written yet. */
    fun newCapturePath(): String

    /** The capture, upright and downscaled. */
    suspend fun read(path: String): AppResult<CaptureImage>

    /**
     * Copies an image out of the system picker into app storage, so the journal keeps
     * working after the picker's temporary grant expires.
     *
     * @param source whatever the platform picker handed back — a `content://` URI on
     *   Android, a file URL on iOS. Callers pass it through untouched.
     * @return the path of the copy, inside app storage.
     */
    suspend fun importFromPicker(source: String): AppResult<String>

    suspend fun delete(path: String?)

    /**
     * Rewrites the capture so its pixels are upright, and drops the rotation tag that said so.
     *
     * [read] already hands out upright bytes, which is enough for anything the app sends to
     * Gemini — but it does not touch the file, and the file is what a photo viewer draws.
     * A capture straight off the camera carries its rotation as EXIF metadata rather than in
     * the pixels, so any renderer that ignores the tag shows it on its side.
     *
     * Called for photos the traveller keeps and looks at rather than ones the app merely
     * uploads: those are worth the re-encode to make upright a property of the file itself.
     */
    suspend fun flattenOrientation(path: String): AppResult<Unit>

    /**
     * Every capture file currently on disk, whether or not anything still points at it.
     *
     * The counterpart to [newCapturePath]: handing out paths is what creates files nothing
     * owns yet, so something has to be able to ask what is actually there. Only the orphan
     * sweep uses this — no screen should be listing storage.
     */
    suspend fun listCaptures(): List<String>
}
