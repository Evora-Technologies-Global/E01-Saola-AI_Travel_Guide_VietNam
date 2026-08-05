package com.duylt.trave.vietlensai.data.repository

import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.dao.TranslationDao
import com.duylt.trave.vietlensai.data.local.file.ImagePolicy
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.repository.CaptureMaintenance
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlin.time.Clock

/**
 * The orphan sweep.
 *
 * Every table that can hold a capture name has to be asked, and the cost of forgetting one
 * is a traveller's photo deleted out from under them — so the three sources are gathered in
 * one place here rather than left to each repository to remember.
 *
 * Works in capture **names** throughout, on both sides of the comparison. `CaptureStore`
 * hands out absolute paths for live use and the database stores names, and this is the one
 * place the two meet — which is why it is also the place that got it wrong and deleted
 * everything. Both guards below exist because of that.
 */
internal class CaptureMaintenanceImpl(
    private val discoveryDao: DiscoveryDao,
    private val noteDao: NoteDao,
    private val translationDao: TranslationDao,
    private val captureStore: CaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
) : CaptureMaintenance {

    override suspend fun sweepOrphans(): Int = withContext(ioDispatcher) {
        val onDisk = captureStore.listCaptures()
        if (onDisk.isEmpty()) return@withContext 0

        // Guarded as one read, and the failure ends the sweep rather than narrowing it.
        // This is the only place in the data layer where a caught exception would be more
        // dangerous than an uncaught one: every path a failed query does not contribute
        // looks exactly like an orphan below, so a `noteDao` that threw while the other
        // two answered would delete every photograph the traveller attached to a note.
        // Deleting nothing costs some storage until the next launch; the other outcome is
        // not recoverable.
        val referenced = try {
            buildSet<String> {
                addAll(discoveryDao.getAllImageNames())
                addAll(translationDao.getAllImageNames())
                noteDao.getAll().forEach { addAll(it.photoNames()) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Could not read which captures are still referenced; sweeping nothing" }
            return@withContext 0
        }

        // The second thing that must be true before anything is deleted: the two sides have
        // to be speaking the same language. Both are capture *names* — never paths — but
        // that is a convention the compiler cannot enforce, and the one time it was broken
        // this sweep deleted every photograph in the app. It looked exactly like this: a
        // database full of references, a disk full of files, and not one name in common.
        //
        // A traveller whose captures really are all orphaned is indistinguishable from that
        // bug, and is also not in a hurry — they lose nothing but some storage until the
        // files age out one at a time alongside a reference that does match.
        if (referenced.isNotEmpty() && onDisk.none { it in referenced }) {
            log.e {
                "Sweeping nothing: ${onDisk.size} capture(s) on disk and " +
                    "${referenced.size} referenced, with no name in common"
            }
            return@withContext 0
        }

        val cutoff = Clock.System.now().toEpochMilliseconds() - GRACE_PERIOD_MILLIS
        val orphans = onDisk.filter { name ->
            if (name in referenced) return@filter false
            // A capture the sweep cannot date is one it cannot prove is stale, and a
            // recent one is more likely mid-recognition than abandoned. Both are kept.
            val createdAt = ImagePolicy.capturedAtMillis(name) ?: return@filter false
            createdAt < cutoff
        }

        orphans.forEach { captureStore.delete(it) }
        if (orphans.isNotEmpty()) {
            log.i { "Swept ${orphans.size} orphaned capture(s)" }
        }
        orphans.size
    }

    private companion object {
        /**
         * How long a capture is left alone before it counts as abandoned.
         *
         * Long enough to cover the whole write → recognise → persist round trip on a bad
         * connection, including a retry. Anything shorter risks the sweep racing a photo
         * the traveller has only just taken; anything longer only delays a cleanup that
         * nobody is waiting on.
         */
        const val GRACE_PERIOD_MILLIS = 15 * 60 * 1000L
    }
}
