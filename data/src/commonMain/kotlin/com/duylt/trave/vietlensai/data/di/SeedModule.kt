package com.duylt.trave.vietlensai.data.di

import com.duylt.trave.vietlensai.data.seed.BundledDemoDataSeeder
import com.duylt.trave.vietlensai.domain.repository.DemoDataSeeder
import kotlinx.coroutines.CoroutineDispatcher
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Which [DemoDataSeeder] the app gets, decided once from the build type.
 *
 * This is the whole of the "debug seeds, release does not" feature at the wiring level, and
 * it is a binding rather than an `if` inside the seeder on purpose: a release build then has
 * no path to the seeding code at all, and `MainViewModel` needs no knowledge of build types
 * to call something on every launch.
 *
 * Modelled on [networkModule], for the same reason — `:data` and `:shared` are single-variant
 * multiplatform modules, so neither can work out which of `:app`'s build types it landed in.
 * The flag arrives from the entry point: `BuildConfig.DEBUG` on Android, `#if DEBUG` on iOS.
 *
 * @param isDebug true only for a development build.
 */
fun seedModule(isDebug: Boolean): Module = module {

    single<DemoDataSeeder> {
        if (!isDebug) {
            // `fun interface`, so the release binding is genuinely nothing: no assets read,
            // no database touched, no branch taken on a launch path everything else waits on.
            DemoDataSeeder { }
        } else {
            BundledDemoDataSeeder(
                assets = get(),
                discoveryDao = get(),
                chatDao = get(),
                tripSummaryDao = get(),
                captureStore = get(),
                ioDispatcher = get<CoroutineDispatcher>(IoDispatcher),
            )
        }
    }
}
