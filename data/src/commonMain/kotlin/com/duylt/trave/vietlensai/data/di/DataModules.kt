package com.duylt.trave.vietlensai.data.di

import org.koin.core.module.Module

/**
 * Everything `:data` contributes to the object graph.
 *
 * Self-contained apart from two things. On Android, [platformDataModule] resolves the
 * application `Context` through Koin's `androidContext()`, so the Android entry point has
 * to call `androidContext(...)` when it starts Koin; iOS needs nothing. And [isDebug] has
 * to be handed in, because a module compiled once for both build types cannot work it out
 * for itself — see [networkModule].
 *
 * @param isDebug true only for a development build.
 */
fun dataModules(isDebug: Boolean): List<Module> = listOf(
    coreModule,
    networkModule(isDebug),
    databaseModule,
    repositoryModule,
    platformDataModule,
)
