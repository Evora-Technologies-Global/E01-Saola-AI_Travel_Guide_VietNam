package com.duylt.trave.vietlensai.data.di

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.duylt.trave.vietlensai.data.local.db.VietLensDatabase
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module

/**
 * The one seam where the two platforms differ.
 *
 * Everything that needs an Android `Context`, an `NSBundle` or a documents directory is
 * bound here; the rest of the graph is declared once in common code and cannot tell the
 * platforms apart. This is deliberately a *module* rather than a set of `expect` classes:
 * the implementations genuinely need different constructor arguments, and pretending
 * otherwise would mean inventing a wrapper type whose only job is to carry a `Context`
 * through code that has no use for one.
 *
 * The bindings each actual must provide:
 *  - [com.duylt.trave.vietlensai.data.local.file.ImageStorage]
 *  - [com.duylt.trave.vietlensai.data.local.asset.BundledAssets]
 *  - [com.duylt.trave.vietlensai.domain.repository.TextRecognizer]
 *  - [com.duylt.trave.vietlensai.domain.repository.LocationRepository]
 *  - [VietLensDatabase]
 *  - `DataStore<Preferences>`
 */
internal expect val platformDataModule: Module

/**
 * The persistence policy both platforms share.
 *
 * Only the file path is platform business; the migration, the fallback and the driver are
 * decided here so the two platforms cannot drift into different storage behaviour.
 */
internal fun RoomDatabase.Builder<VietLensDatabase>.applySharedConfiguration(
    ioDispatcher: CoroutineDispatcher,
): RoomDatabase.Builder<VietLensDatabase> = this
    // v2 adds provinceId for the passport map, v3 the traveller's own notes. Both are
    // additive, so both get real migrations — losing every recorded discovery is too high
    // a price for one nullable column, and losing what the traveller wrote is worse still.
    .addMigrations(VietLensDatabase.MIGRATION_1_2, VietLensDatabase.MIGRATION_2_3)
    // Pre-1.0: any *other* schema change should reset local history rather than block
    // the build on a migration no shipped install will ever need.
    .fallbackToDestructiveMigration(dropAllTables = true)
    // The bundled driver, not the platform one: it ships the same SQLite build to both
    // platforms, so a query cannot behave differently because the OS happens to link an
    // older library. It is also the only driver available on Kotlin/Native.
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(ioDispatcher)
