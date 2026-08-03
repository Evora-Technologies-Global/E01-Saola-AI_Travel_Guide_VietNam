package com.duylt.trave.vietlensai.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.duylt.trave.vietlensai.data.local.db.entity.ChatMessageEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryEntity
import com.duylt.trave.vietlensai.data.local.db.entity.DiscoveryNoteEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TranslationEntity
import com.duylt.trave.vietlensai.data.local.db.entity.TripSummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiscoveryDao {

    @Query("SELECT * FROM discoveries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DiscoveryEntity>>

    @Query("SELECT * FROM discoveries WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun observeFavorites(): Flow<List<DiscoveryEntity>>

    @Query("SELECT * FROM discoveries WHERE id = :id")
    fun observeById(id: String): Flow<DiscoveryEntity?>

    @Query("SELECT * FROM discoveries WHERE id = :id")
    suspend fun getById(id: String): DiscoveryEntity?

    /**
     * Newest first, capped.
     *
     * The cap is vestigial: the one surviving caller is `deleteAll`, which passes
     * `Int.MAX_VALUE` because it wants every row — it walks them to delete the capture file
     * behind each discovery before the rows go. The journal prompt is built from
     * [getBetween], not from here, whatever the ordering suggests.
     */
    @Query("SELECT * FROM discoveries ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<DiscoveryEntity>

    @Query("SELECT * FROM discoveries WHERE createdAt BETWEEN :fromEpochMillis AND :toEpochMillis ORDER BY createdAt ASC")
    suspend fun getBetween(fromEpochMillis: Long, toEpochMillis: Long): List<DiscoveryEntity>

    @Upsert
    suspend fun upsert(entity: DiscoveryEntity)

    @Query("UPDATE discoveries SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: String)

    @Query("DELETE FROM discoveries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM discoveries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM discoveries")
    fun observeCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM discoveries WHERE isFavorite = 1")
    fun observeFavoriteCount(): Flow<Int>

    /**
     * One row per province the traveller has stamped, for the passport map.
     *
     * The cover photo prefers a favourite over the newest capture: if someone
     * marked one shot in Đà Nẵng as their favourite, that is the one they want
     * filling the province, not whatever they happened to point the camera at last.
     */
    @Query(
        """
        SELECT d.provinceId AS provinceId,
               COUNT(*) AS discoveryCount,
               MIN(d.createdAt) AS firstVisitAt,
               MAX(d.createdAt) AS lastVisitAt,
               (
                   SELECT cover.imageName FROM discoveries AS cover
                   WHERE cover.provinceId = d.provinceId AND cover.imageName IS NOT NULL
                   ORDER BY cover.isFavorite DESC, cover.createdAt DESC
                   LIMIT 1
               ) AS coverImageName
        FROM discoveries AS d
        WHERE d.provinceId IS NOT NULL
        GROUP BY d.provinceId
        """,
    )
    fun observeProvinceStamps(): Flow<List<ProvinceStampRow>>

    @Query("SELECT * FROM discoveries WHERE provinceId = :provinceId ORDER BY createdAt DESC")
    fun observeByProvince(provinceId: String): Flow<List<DiscoveryEntity>>

    /** Rows recorded before the passport existed, which still know where they were taken. */
    @Query(
        """
        SELECT id AS id, latitude AS latitude, longitude AS longitude
        FROM discoveries
        WHERE provinceId IS NULL AND latitude IS NOT NULL AND longitude IS NOT NULL
        """,
    )
    suspend fun getUnstamped(): List<UnstampedRow>

    @Query("UPDATE discoveries SET provinceId = :provinceId WHERE id = :id")
    suspend fun setProvinceId(id: String, provinceId: String?)

    /** Every capture still spoken for by a discovery, for the orphan sweep. */
    @Query("SELECT imageName FROM discoveries WHERE imageName IS NOT NULL")
    suspend fun getAllImageNames(): List<String>
}

/** Roll-up of everything found in one province. */
data class ProvinceStampRow(
    val provinceId: String,
    val discoveryCount: Int,
    val firstVisitAt: Long,
    val lastVisitAt: Long,
    val coverImageName: String?,
)

/** A located discovery that has not been assigned to a province yet. */
data class UnstampedRow(
    val id: String,
    val latitude: Double,
    val longitude: Double,
)

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE discoveryId = :discoveryId ORDER BY createdAt ASC")
    fun observeThread(discoveryId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE discoveryId = :discoveryId ORDER BY createdAt ASC")
    suspend fun getThread(discoveryId: String): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE discoveryId = :discoveryId")
    suspend fun deleteThread(discoveryId: String)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface NoteDao {

    @Query("SELECT * FROM discovery_notes WHERE discoveryId = :discoveryId")
    fun observeByDiscovery(discoveryId: String): Flow<DiscoveryNoteEntity?>

    @Query("SELECT * FROM discovery_notes WHERE discoveryId = :discoveryId")
    suspend fun getByDiscovery(discoveryId: String): DiscoveryNoteEntity?

    /**
     * The notes attached to a set of discoveries, for the diary prompt.
     *
     * Takes ids rather than a time range because the journal has already cut the day in
     * the device's own time zone — re-deriving that cut from `createdAt` here would let a
     * note written the morning after fall into the wrong day.
     */
    @Query("SELECT * FROM discovery_notes WHERE discoveryId IN (:discoveryIds)")
    suspend fun getForDiscoveries(discoveryIds: List<String>): List<DiscoveryNoteEntity>

    @Query("SELECT * FROM discovery_notes")
    suspend fun getAll(): List<DiscoveryNoteEntity>

    @Upsert
    suspend fun upsert(entity: DiscoveryNoteEntity)

    @Query("DELETE FROM discovery_notes WHERE discoveryId = :discoveryId")
    suspend fun deleteByDiscovery(discoveryId: String)

    @Query("SELECT COUNT(*) FROM discovery_notes")
    fun observeCount(): Flow<Int>
}

@Dao
interface TranslationDao {

    @Query("SELECT * FROM translations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TranslationEntity>>

    @Query("SELECT * FROM translations WHERE id = :id")
    fun observeById(id: String): Flow<TranslationEntity?>

    @Upsert
    suspend fun upsert(entity: TranslationEntity)

    @Query("DELETE FROM translations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM translations")
    suspend fun deleteAll()

    /**
     * Every capture still spoken for by a translation, for the orphan sweep.
     *
     * Easy to forget that translations hold captures too — the sweep that omitted this
     * would quietly delete the photo behind every saved menu translation.
     */
    @Query("SELECT imageName FROM translations WHERE imageName IS NOT NULL")
    suspend fun getAllImageNames(): List<String>
}

@Dao
interface TripSummaryDao {

    @Query("SELECT * FROM trip_summaries ORDER BY date DESC")
    fun observeAll(): Flow<List<TripSummaryEntity>>

    @Query("SELECT * FROM trip_summaries WHERE date = :date")
    suspend fun getByDate(date: String): TripSummaryEntity?

    @Upsert
    suspend fun upsert(entity: TripSummaryEntity)

    @Query("DELETE FROM trip_summaries")
    suspend fun deleteAll()
}
