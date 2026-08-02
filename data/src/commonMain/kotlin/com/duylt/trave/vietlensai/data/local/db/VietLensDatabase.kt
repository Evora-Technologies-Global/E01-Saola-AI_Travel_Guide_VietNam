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
    // Back to 1, and there are no migrations. The schema reached 3 by adding `provinceId`
    // and then `discovery_notes`, but the app has never been published, so there is no
    // install anywhere holding a v1 or v2 file worth migrating — carrying the two
    // migrations forward would mean maintaining upgrade paths from shapes that only ever
    // existed on a developer's phone.
    //
    // What this does to a phone that already has the v3 file is worth being exact about,
    // because it is not a crash: `applySharedConfiguration` calls
    // `fallbackToDestructiveMigration(dropAllTables = true)`, and that setter also sets
    // `allowDestructiveMigrationOnDowngrade`. Room therefore treats 3 -> 1 as "no migration
    // required", drops every table and recreates them empty. The database opens, the app
    // starts, and the journal, notes, translations and passport stamps on that device are
    // simply gone — silently, with nothing shown to the user. Acceptable only because
    // nothing is published; a device carrying demo data has to be re-seeded.
    version = 1,
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
