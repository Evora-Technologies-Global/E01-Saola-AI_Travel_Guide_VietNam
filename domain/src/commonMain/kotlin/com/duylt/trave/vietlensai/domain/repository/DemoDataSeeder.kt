package com.duylt.trave.vietlensai.domain.repository

/**
 * Fills an empty journal with the demo trip, so a development build opens on something worth
 * looking at instead of an empty state.
 *
 * A port rather than a use case, and declared here rather than in `:data`, because the window
 * host is what calls it and `:shared` cannot see `:data`. There is deliberately **no**
 * `DiscoveryRepository.insert` to do this through: giving the domain a way to write a
 * discovery that Gemini did not produce would hand every ViewModel the same ability, and the
 * one property that makes the journal trustworthy is that everything in it was really
 * recognised. The seeder writes below that layer instead.
 *
 * **Which implementation is bound is decided once, at startup, from the build type.** A
 * release build gets the no-op — see `seedModule` in `:data`. `:data` and `:shared` are
 * single-variant multiplatform modules, so neither can work out for itself which of `:app`'s
 * build types it landed in; the flag has to arrive from the entry point.
 */
fun interface DemoDataSeeder {

    /**
     * Writes the demo trip if, and only if, there is nothing in the journal yet.
     *
     * Emptiness is the whole condition, and there is no "already seeded" flag beside it. That
     * makes **Settings → clear everything** the reset button for a demo build: wipe it, relaunch,
     * and the trip is back. The cost is that an empty journal cannot be held open on a debug
     * build to look at an empty state — do that on a release build, which never seeds.
     *
     * Never throws. Seeding is a convenience with no user waiting on it, so a failure is
     * logged and the app opens on whatever is really there.
     */
    suspend fun seedIfEmpty()
}
