package com.duylt.trave.vietlensai

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.duylt.trave.vietlensai.di.appModules
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.verify.MissingKoinDefinitionException
import org.koin.test.verify.verify
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * The Koin graph `VietLensApplication` starts, checked without starting it.
 *
 * **This is `:app`'s first test and the module had none, which is the reason it is this one.**
 * `:app` is two files of glue — an `Application` that calls `startKoin` and an `Activity` that
 * calls `setContent` — so almost nothing here is unit-testable in the usual sense. What *is*
 * this module's own is the assembly: it is the only place in the Android build that composes
 * `appModules(BuildConfig.DEBUG)` and hands the graph a `Context`, and a definition whose
 * constructor cannot be satisfied does not fail there. It fails later, on a device, the first
 * time a traveller opens the screen that asks for it — every ViewModel is a `factory`, so a
 * missing binding under the passport stays invisible until somebody navigates to the passport.
 *
 * `verify()` walks each definition's constructor by reflection and resolves every parameter
 * against the rest of the graph. That is exactly the failure above, and it is why this runs on
 * the JVM rather than as an instrumented test: nothing is constructed, no database is opened,
 * no HTTP client is built, no Android class is touched.
 *
 * **The assertion is [injectedTypes] being short.** `verify` throws when a definition needs a
 * type the graph neither defines nor is handed, so every entry on that list is a claim that
 * something outside Koin really does supply it. A type added there to make a red test go green
 * is the test being switched off.
 */
class AppGraphTest {

    /**
     * What the graph is handed rather than what it builds, and why each one is allowed.
     *
     * - `Context` — `androidContext(this@VietLensApplication)` in `VietLensApplication`. `:data`
     *   resolves the database path, the asset manager and the location provider through it.
     * - `SavedStateHandle` — supplied per ViewModel by the navigation library from the
     *   back-stack entry. The discovery, chat, translation and passport ViewModels each read a
     *   route argument out of it.
     *
     * Anything else appearing here later is a real finding, not a fix: it means a definition is
     * asking for something no module provides, and the only reason the app still runs is that
     * nobody has opened that screen yet.
     */
    private val injectedTypes = listOf(Context::class, SavedStateHandle::class)

    /**
     * The types `verify` is wrong about, kept apart from [injectedTypes] because the reason is
     * different in kind and only one of the two lists is worth arguing about.
     *
     * `Context` and `SavedStateHandle` are genuinely supplied from outside Koin. These two are
     * supplied by the graph — `verify` simply cannot see it, because it reflects on a
     * definition's *constructor* rather than on the `single { }` lambda that fills it in:
     *
     * - `io.ktor.client.engine.HttpClientEngine` — `NetworkModule.kt:40` writes
     *   `HttpClient(httpClientEngine())`, and `httpClientEngine()` is an `expect fun`, OkHttp
     *   on Android and Darwin on iOS. It is resolved by the compiler and never by Koin, so
     *   there is no definition to find and none is needed.
     * - `androidx.datastore.core.DataStore` — `PlatformModule.android.kt:58` defines
     *   `single<DataStore<Preferences>>`, and `SettingsDataStore` asks for exactly that. The
     *   constructor parameter reflects as the *raw* `DataStore`, which matches no key, so this
     *   is generic erasure rather than a missing binding.
     *
     * **Named as strings on purpose.** Both belong to `:data`, which exposes Ktor and DataStore
     * as `implementation` — correctly, since nothing else should reach for them. Importing them
     * here to write `::class` would mean adding two compile dependencies to `:app` for the sake
     * of a test, which is a worse trade than one `Class.forName` per entry; they are on the
     * runtime classpath either way. A name that stops resolving fails this test loudly, which
     * is the right outcome: it means the definition it excuses has moved.
     */
    private val reflectionBlindSpots = listOf(
        "io.ktor.client.engine.HttpClientEngine",
        "androidx.datastore.core.DataStore",
    ).map { Class.forName(it).kotlin }

    private val allowed = injectedTypes + reflectionBlindSpots

    /**
     * All eight modules as one, which is the only shape `verify` can actually check.
     *
     * **`verifyAll(modules)` is not it, despite the name.** In Koin 4.2.2 that function is
     * `forEach { it.verify(...) }` — each module is resolved against its own definitions alone,
     * so every legitimate cross-module edge reports as missing: the repositories asking
     * `:data`'s core module for a dispatcher, the use cases asking for repositories, ten
     * ViewModels asking for use cases. Nearly the whole graph, all of it a false alarm.
     *
     * `includes` is what fixes it. `Verification` flattens a module's included modules before
     * it resolves anything, so one wrapper puts every definition in scope at once — which is
     * also the arrangement Koin itself sees at `startKoin`.
     */
    private fun graph(isDebug: Boolean): Module = module { includes(appModules(isDebug)) }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every definition in the debug graph can be constructed`() {
        graph(isDebug = true).verify(extraTypes = allowed)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `every definition in the release graph can be constructed`() {
        // Verified separately rather than assumed to be the same shape. `isDebug` is threaded
        // into `:data`'s `networkModule(isDebug)`, which builds a *different module instance*
        // per build type — the one branch in the whole graph that the debug pass cannot reach.
        // A release APK is also the one nobody runs from an IDE before shipping it.
        graph(isDebug = false).verify(extraTypes = allowed)
    }

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `the check is not vacuous - a graph missing its data layer fails`() {
        // Everything above `:data`: the use cases, the ViewModels, the platform UI bindings.
        // Every use case takes a repository, so this graph cannot resolve and `verify` has to
        // say so. Without this case the three above would pass just as happily if `verify`
        // silently stopped checking — which is not hypothetical, since it *did* stop checking
        // when they were written against `verifyAll`, where each module saw only its own
        // definitions and the whole graph read as broken. A checker can fail in either
        // direction and only one of them is loud.
        val aboveTheDataLayer = module { includes(appModules(isDebug = true).takeLast(3)) }

        assertFailsWith<MissingKoinDefinitionException> {
            aboveTheDataLayer.verify(extraTypes = allowed)
        }
    }

    @Test
    fun `the graph is assembled from all four layers`() {
        // A guard on the one line that can silently drop a whole layer:
        // `dataModules(isDebug) + useCaseModule + presentationModule + platformUiModule`.
        // Dropping a summand still compiles and `startKoin` still succeeds; the app then dies
        // on the first screen that needed the missing half. Counted rather than named because
        // the names are `internal` to `:shared` and `:data` — what this pins is the arithmetic.
        val expected = 5 + 1 + 1 + 1 // data's five, use cases, presentation, platform UI
        assertEquals(expected, appModules(isDebug = true).size)
        assertEquals(expected, appModules(isDebug = false).size)
    }
}
