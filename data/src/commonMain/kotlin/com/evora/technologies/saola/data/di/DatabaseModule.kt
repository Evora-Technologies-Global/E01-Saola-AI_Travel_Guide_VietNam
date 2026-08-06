package com.evora.technologies.saola.data.di

import com.evora.technologies.saola.data.local.datastore.SettingsDataStore
import com.evora.technologies.saola.data.local.db.SaolaDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The DAOs, and the settings wrapper around the preferences store.
 *
 * The database and the `DataStore` themselves come from [platformDataModule], because only
 * a platform can say where its files live — everything downstream of that is identical.
 */
val databaseModule: Module = module {

    single { get<SaolaDatabase>().discoveryDao() }
    single { get<SaolaDatabase>().chatDao() }
    single { get<SaolaDatabase>().noteDao() }
    single { get<SaolaDatabase>().reportDao() }
    single { get<SaolaDatabase>().translationDao() }
    single { get<SaolaDatabase>().tripSummaryDao() }

    single { SettingsDataStore(dataStore = get()) }
}
