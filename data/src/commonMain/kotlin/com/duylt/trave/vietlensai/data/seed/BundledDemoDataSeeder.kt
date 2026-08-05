package com.duylt.trave.vietlensai.data.seed

import com.duylt.trave.vietlensai.data.local.asset.BundledAssets
import com.duylt.trave.vietlensai.data.local.db.dao.ChatDao
import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.TripSummaryDao
import com.duylt.trave.vietlensai.data.util.log
import com.duylt.trave.vietlensai.domain.repository.CaptureStore
import com.duylt.trave.vietlensai.domain.repository.DemoDataSeeder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import kotlin.time.Clock

/**
 * Writes the demo trip from assets that only a development build packages.
 *
 * Bound only when `isDebug` is true (see `seedModule`), and even then it can only work if the
 * assets are present — `:app`'s debug variant is the one thing that puts them there. Two
 * independent gates, which is deliberate: a release APK carries neither this decision nor the
 * 3.5 MB of photographs it would need.
 *
 * Writes below the repository layer, straight to the DAOs, because `DiscoveryRepository` has
 * no way to store a discovery that Gemini did not produce and should not gain one — see
 * [DemoDataSeeder].
 */
internal class BundledDemoDataSeeder(
    private val assets: BundledAssets,
    private val discoveryDao: DiscoveryDao,
    private val chatDao: ChatDao,
    private val tripSummaryDao: TripSummaryDao,
    private val captureStore: CaptureStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) : DemoDataSeeder {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun seedIfEmpty() {
        try {
            if (discoveryDao.observeCount().first() > 0) return

            val raw = assets.readText(DEMO_CONTENT_ASSET)
            if (raw == null) {
                log.i { "No demo content packaged in this build; opening on a real journal" }
                return
            }

            val trip = json.decodeFromString<DemoContent>(raw)
                .toTrip(nowMillis = Clock.System.now().toEpochMilliseconds(), timeZone = TimeZone.currentSystemDefault())

            // Photographs first. A row whose photograph failed to land would draw a broken
            // tile on the passport and the collection board, so a discovery is only written
            // once its picture is on disk.
            var written = 0
            for (seeded in trip.discoveries) {
                val bytes = assets.readBytes(seeded.photoAsset)
                if (bytes == null) {
                    log.w { "Demo photograph ${seeded.photoAsset} is missing; skipping that find" }
                    continue
                }
                writeCapture(seeded.entity.imageName ?: continue, bytes)
                discoveryDao.upsert(seeded.entity)
                written++
            }

            // Only after the discoveries: chat rows carry a foreign key onto them, and a
            // summary for a day with nothing in it reads as a diary entry about an empty day.
            if (written > 0) {
                trip.messages.forEach { chatDao.insert(it) }
                trip.summaries.forEach { tripSummaryDao.upsert(it) }
            }

            log.i { "Seeded $written demo discoveries, ${trip.messages.size} chat turns" }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            // Nobody is waiting on this and nothing downstream depends on it. A half-written
            // trip is still a usable journal, and the next launch finds the table non-empty.
            log.w(e) { "Could not seed the demo trip" }
        }
    }

    private suspend fun writeCapture(name: String, bytes: ByteArray) = withContext(ioDispatcher) {
        // `resolve` is the only supported way to turn a stored name back into a path, and it
        // creates the captures directory on both platforms as a side effect of answering.
        val path = captureStore.resolve(name).toPath()
        fileSystem.write(path) { write(bytes) }
    }
}
