package com.duylt.trave.vietlensai.data.di

import kotlinx.coroutines.CoroutineDispatcher
import com.duylt.trave.vietlensai.data.platform.platformIoDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Marks the dispatcher used for disk and network work, so tests can swap it.
 *
 * A Koin qualifier rather than a `@Qualifier` annotation: Dagger's qualifiers need an
 * annotation processor, and no processor runs for Kotlin/Native. Callers reference this
 * constant instead of the string, so a typo is a compile error rather than a
 * missing-binding crash at startup.
 */
val IoDispatcher = named("io-dispatcher")

/** An application-lifetime scope for work that must outlive any one screen. */
val ApplicationScope = named("application-scope")

val coreModule: Module = module {

    single<CoroutineDispatcher>(IoDispatcher) { platformIoDispatcher() }

    single<CoroutineScope>(ApplicationScope) {
        CoroutineScope(SupervisorJob() + get<CoroutineDispatcher>(IoDispatcher))
    }

    /**
     * Injected rather than read at each call site so day boundaries in the journal can
     * be driven from a fixed zone in tests.
     */
    single<TimeZone> { TimeZone.currentSystemDefault() }

    /**
     * [Json.ignoreUnknownKeys] is not optional here: Gemini adds response fields
     * between model revisions (thought signatures being the most recent example),
     * and a strict parser would turn every such addition into a crash.
     */
    single<Json> {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
            encodeDefaults = false
            isLenient = true
            coerceInputValues = true
        }
    }
}
