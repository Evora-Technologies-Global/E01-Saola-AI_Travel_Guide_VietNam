package com.evora.technologies.saola.data.repository

import com.evora.technologies.saola.data.util.log
import com.evora.technologies.saola.domain.util.AppError
import com.evora.technologies.saola.domain.util.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/*
 * The three guards that stop local storage from throwing across a repository boundary.
 *
 * `AppResult` promises that "an unreachable network, a throttled model or an unreadable
 * photo are all outcomes the UI has to render". Room has never heard of that promise. A
 * database opened while the device was still locked, a page SQLite cannot read, a
 * `SerializationException` from a JSON column written by a schema that has since moved —
 * each of those arrives as a throw, and a throw out of a method whose return type is
 * `AppResult` is the one failure nobody downstream has written a branch for.
 *
 * Writes in this package were already wrapped. Reads and Flows were not, and that
 * asymmetry is what these exist to remove: the read that fails is the one that runs at
 * launch, on the journal screen, before the traveller has touched anything.
 *
 * They live in one file rather than in one repository because the same few lines belong in
 * all of them, and because the line easiest to leave out is the one that matters most —
 * [CancellationException] is an `Exception`, so a bare `catch (e: Exception)` swallows the
 * signal that tells a coroutine to stop. That breaks structured concurrency and reports a
 * screen the traveller has already left as a storage failure.
 */

/**
 * Runs a storage operation and folds anything it throws into [AppError.Storage].
 *
 * @param what names the operation in the log, phrased to follow "Could not" — a stack
 *   trace with no idea which of a screen's four queries produced it costs an afternoon.
 */
internal inline fun <T> runCatchingStorage(what: String, block: () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log.e(e) { "Could not $what" }
    AppResult.Failure(AppError.Storage(e.message))
}

/**
 * The same guard for the reads whose signature has nowhere to put a failure.
 *
 * `getDiscovery`, `sweepOrphans` and `backfillProvinces` all return a plain value, so the
 * only thing a broken database can be reported as is the value that means "nothing".
 * Every one of their callers already renders that state, which is why it is an acceptable
 * answer — but it has to be a deliberate, logged choice rather than an exception that
 * happened to reach a screen.
 */
internal inline fun <T> runCatchingStorageOr(what: String, fallback: T, block: () -> T): T = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log.e(e) { "Could not $what" }
    fallback
}

/**
 * Keeps a Flow alive when the query behind it, or the mapper after it, fails.
 *
 * A Flow that throws is worse than a failed call. The collector dies with it, the screen
 * is left permanently blank, and nothing short of killing the app will make it try again —
 * there is no retry button for a subscription that no longer exists. Emitting [fallback]
 * instead means the screen renders its empty state, which at least reads as "nothing here
 * yet" and recovers by itself the moment the next emission arrives.
 *
 * Placed at the *end* of a chain rather than next to the DAO, so it covers the entity
 * mapping too — decoding a column is exactly where an older row goes wrong.
 */
internal fun <T> Flow<T>.fallbackOnFailure(fallback: T, what: String): Flow<T> =
    catch { throwable ->
        if (throwable is CancellationException) throw throwable
        log.e(throwable) { "Could not $what" }
        emit(fallback)
    }
