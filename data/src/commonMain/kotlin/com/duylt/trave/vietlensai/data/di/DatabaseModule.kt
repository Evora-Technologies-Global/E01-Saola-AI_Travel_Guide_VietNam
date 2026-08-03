package com.duylt.trave.vietlensai.data.di

import com.duylt.trave.vietlensai.data.local.datastore.SettingsDataStore
import com.duylt.trave.vietlensai.data.local.db.VietLensDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The DAOs, and the settings wrapper around the preferences store.
 *
 * The database and the `DataStore` themselves come from [platformDataModule], because only
 * a platform can say where its files live — everything downstream of that is identical.
 */
val databaseModule: Module = module {

    single { get<VietLensDatabase>().discoveryDao() }
    single { get<VietLensDatabase>().chatDao() }
    single { get<VietLensDatabase>().noteDao() }
    single { get<VietLensDatabase>().translationDao() }
    single { get<VietLensDatabase>().tripSummaryDao() }

    single { SettingsDataStore(dataStore = get()) }
}
