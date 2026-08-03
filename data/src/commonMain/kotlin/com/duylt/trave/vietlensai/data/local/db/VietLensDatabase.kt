package com.duylt.trave.vietlensai.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.duylt.trave.vietlensai.data.local.db.dao.ChatDao
import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.dao.TranslationDao
import com.duylt.trave.vietlensai.data.local.db.dao.TripSummaryDao
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity

@Database(
    entities = [
        DiscoveryEntity::class,
        ChatMessageEntity::class,
        DiscoveryNoteEntity::class,
        TranslationEntity::class,
        TripSummaryEntity::class,
    ],
    // 2, and there are still no migrations. It had been reset to 1 once already, for the
    // reason below; the bump to 2 exists because `imagePath` became `imageName` and
    // `photoPathsJson` became `photoNamesJson` — and those are not renames for tidiness.
    // The columns used to hold absolute paths, which on iOS name a container directory
    // that does not survive an install, an update or a restore. Every row written before
    // this version points somewhere that no longer exists, so there is nothing in one
    // worth carrying over even if a migration were written. Destroying them is the fix.
    //
    // A bump is what makes that happen: renaming a column does not change the *version*,
    // and destructive fallback only runs on a version change — same-version schema drift
    // is an integrity failure Room throws on instead.
    //
    // What this does to a phone that already has the v3 file is worth being exact about,
    // because it is not a crash: `applySharedConfiguration` calls
    // `fallbackToDestructiveMigration(dropAllTables = true)`, and that setter also sets
    // `allowDestructiveMigrationOnDowngrade`. Room therefore treats any version it has no
    // migration for — in either direction — as "no migration required", drops every table
    // and recreates them empty. The database opens, the app starts, and the journal, notes,
    // translations and passport stamps on that device are simply gone — silently, with
    // nothing shown to the user. Acceptable only because nothing is published; a device
    // carrying demo data has to be re-seeded.
    version = 2,
    exportSchema = true,
)
// Room generates the implementation per target rather than through reflection, so
// the multiplatform build needs a declared constructor to hang the generated
// `actual` off. The body stays empty — KSP fills it in for each platform.
@ConstructedBy(VietLensDatabaseConstructor::class)
abstract class VietLensDatabase : RoomDatabase() {

    abstract fun discoveryDao(): DiscoveryDao
    abstract fun chatDao(): ChatDao
    abstract fun noteDao(): NoteDao
    abstract fun translationDao(): TranslationDao
    abstract fun tripSummaryDao(): TripSummaryDao

    companion object {
        const val NAME = "vietlens.db"
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
expect object VietLensDatabaseConstructor : RoomDatabaseConstructor<VietLensDatabase> {
    override fun initialize(): VietLensDatabase
}
