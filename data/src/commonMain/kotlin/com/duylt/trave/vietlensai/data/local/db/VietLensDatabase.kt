package com.duylt.trave.vietlensai.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.duylt.trave.vietlensai.data.local.db.dao.ChatDao
import com.duylt.trave.vietlensai.data.local.db.dao.DiscoveryDao
import com.duylt.trave.vietlensai.data.local.db.dao.NoteDao
import com.duylt.trave.vietlensai.data.local.db.dao.RecommendationDao
import com.duylt.trave.vietlensai.data.local.db.dao.TranslationDao
import com.duylt.trave.vietlensai.data.local.db.dao.TripSummaryDao
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.local.db.entity.RecommendationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity

@Database(
    entities = [
        DiscoveryEntity::class,
        ChatMessageEntity::class,
        DiscoveryNoteEntity::class,
        TranslationEntity::class,
        RecommendationEntity::class,
        TripSummaryEntity::class,
    ],
    version = 3,
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
    abstract fun recommendationDao(): RecommendationDao
    abstract fun tripSummaryDao(): TripSummaryDao

    companion object {
        const val NAME = "vietlens.db"

        /**
         * Adds `provinceId` for the passport map.
         *
         * Written as a real migration rather than left to the destructive fallback
         * the rest of the schema relies on: the column is additive, and wiping a
         * traveller's entire journal to gain a map of where they have been would be
         * a poor trade. Existing rows are stamped afterwards by
         * `ProvinceRepository.backfillProvinces`, which is why the column is added
         * nullable rather than with a default.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE discoveries ADD COLUMN provinceId TEXT")
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_discoveries_provinceId " +
                        "ON discoveries (provinceId)",
                )
            }
        }

        /**
         * Adds `discovery_notes` — the traveller's own writing.
         *
         * A real migration for the same reason [MIGRATION_1_2] is one, only more so: this
         * table is the only content in the database the traveller authored rather than
         * received, so the destructive fallback is the one outcome it must never take.
         * Purely additive — no existing table is touched, so nothing can be lost applying it.
         *
         * The DDL is written out in the exact shape Room generates for
         * [DiscoveryNoteEntity], including the quoting and the `NO ACTION` clause: Room
         * compares the migrated schema against the generated one on open and throws if they
         * differ by so much as a default.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `discovery_notes` (
                        `discoveryId` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `photoPathsJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`discoveryId`),
                        FOREIGN KEY(`discoveryId`) REFERENCES `discoveries`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                connection.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_discovery_notes_createdAt` " +
                        "ON `discovery_notes` (`createdAt`)",
                )
            }
        }
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
