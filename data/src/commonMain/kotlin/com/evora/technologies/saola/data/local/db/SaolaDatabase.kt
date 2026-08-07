package com.evora.technologies.saola.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.evora.technologies.saola.data.local.db.dao.ChatDao
import com.evora.technologies.saola.data.local.db.dao.DiscoveryDao
import com.evora.technologies.saola.data.local.db.dao.NoteDao
import com.evora.technologies.saola.data.local.db.dao.ReportDao
import com.evora.technologies.saola.data.local.db.dao.TranslationDao
import com.evora.technologies.saola.data.local.db.dao.TripSummaryDao
import com.evora.technologies.saola.data.local.db.entity.ChatMessageEntity
import com.evora.technologies.saola.data.local.db.entity.DiscoveryEntity
import com.evora.technologies.saola.data.local.db.entity.DiscoveryNoteEntity
import com.evora.technologies.saola.data.local.db.entity.DiscoveryReportEntity
import com.evora.technologies.saola.data.local.db.entity.TranslationEntity
import com.evora.technologies.saola.data.local.db.entity.TripSummaryEntity

@Database(
    entities = [
        DiscoveryEntity::class,
        ChatMessageEntity::class,
        DiscoveryNoteEntity::class,
        DiscoveryReportEntity::class,
        TranslationEntity::class,
        TripSummaryEntity::class,
    ],
    // **1, and it stays at 1 until the app is published.** Nothing is on a store, so there
    // is no install anywhere whose rows have to survive — which makes a version number a
    // record of nothing. It had reached 3 (2 for the `imagePath` → `imageName` rename,
    // 3 for `discovery_reports`), and every one of those bumps was destructive anyway:
    // each existed only to *trigger* the fallback below, never to carry data across.
    // Counting up while destroying everything at each step describes a migration history
    // that does not exist.
    //
    // So the rule while unpublished is: **change the schema freely, leave the number
    // alone.** There is one thing that rule cannot do, and it is worth knowing before
    // trusting it — Room hashes the schema into the file and compares it on open, so a
    // *same-version* change is an integrity failure it throws on rather than a fallback
    // it runs. A developer whose device holds an older `saola.db` therefore has to
    // uninstall, or clear app data, rather than expecting the app to recover; on a debug
    // build the demo trip re-seeds itself on the next launch and the state is back.
    //
    // The same applies to the reset itself: a device carrying the v2 or v3 file is being
    // *downgraded*. That path does work — `applySharedConfiguration` calls
    // `fallbackToDestructiveMigration(dropAllTables = true)`, and that setter also sets
    // `allowDestructiveMigrationOnDowngrade`, so Room treats a version it has no migration
    // for in either direction as "no migration required", drops every table and recreates
    // them empty. The database opens, the app starts, and the journal, notes, translations
    // and passport stamps on that device are simply gone — silently, with nothing shown to
    // the user.
    //
    // **Publishing is what ends this.** At that point the rows stop being disposable: the
    // number starts moving again, the first real `Migration` is written, and
    // `fallbackToDestructiveMigration` comes out of `applySharedConfiguration`.
    version = 1,
    exportSchema = true,
)
// Room generates the implementation per target rather than through reflection, so
// the multiplatform build needs a declared constructor to hang the generated
// `actual` off. The body stays empty — KSP fills it in for each platform.
@ConstructedBy(SaolaDatabaseConstructor::class)
abstract class SaolaDatabase : RoomDatabase() {

    abstract fun discoveryDao(): DiscoveryDao
    abstract fun chatDao(): ChatDao
    abstract fun noteDao(): NoteDao
    abstract fun reportDao(): ReportDao
    abstract fun translationDao(): TranslationDao
    abstract fun tripSummaryDao(): TripSummaryDao

    companion object {
        const val NAME = "saola.db"
    }
}

/**
 * The generated-implementation hook Room's multiplatform mode requires.
 *
 * `expect` with no actual anywhere in this source tree is deliberate: KSP emits the
 * `actual object` for every target it runs against, which is why the compiler is
 * given the opt-in below.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object SaolaDatabaseConstructor : RoomDatabaseConstructor<SaolaDatabase> {
    override fun initialize(): SaolaDatabase
}
