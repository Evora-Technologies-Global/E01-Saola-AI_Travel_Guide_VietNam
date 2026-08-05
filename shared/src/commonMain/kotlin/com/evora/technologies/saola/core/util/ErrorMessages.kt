package com.evora.technologies.saola.core.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import com.evora.technologies.saola.resources.Res
import com.evora.technologies.saola.resources.error_api
import com.evora.technologies.saola.resources.error_image_unavailable
import com.evora.technologies.saola.resources.error_invalid_api_key
import com.evora.technologies.saola.resources.error_location_unavailable
import com.evora.technologies.saola.resources.error_malformed
import com.evora.technologies.saola.resources.error_missing_api_key
import com.evora.technologies.saola.resources.error_models_busy
import com.evora.technologies.saola.resources.error_no_connection
import com.evora.technologies.saola.resources.error_not_recognized
import com.evora.technologies.saola.resources.error_rate_limited
import com.evora.technologies.saola.resources.error_storage
import com.evora.technologies.saola.resources.error_timeout
import com.evora.technologies.saola.resources.error_unexpected
import com.evora.technologies.saola.domain.util.AppError

/**
 * Which words answer a given [AppError] — chosen without deciding how they will be read.
 *
 * The choice is separated from the reading because it is needed in two places that cannot
 * call each other: inside composition, where `stringResource` is available, and inside an
 * effect collector, which is a plain suspend lambda. Keeping one `when` means the two can
 * never drift into telling the traveller different things about the same failure.
 */
private sealed interface ErrorText {

    /** Copy the failure carried with it. Already in the traveller's language — the model wrote it. */
    data class Literal(val value: String) : ErrorText

    /**
     * @param arg the single value a message interpolates, or null for the ones that take none.
     *   An `Int?` rather than a `List<Any>` because exactly one error formats anything — the
     *   HTTP code on [AppError.Api] — and `List<Any>` is unstable to Compose, which would put
     *   this private detail into the unstable-class count the stability gate watches.
     */
    data class FromResources(val id: StringResource, val arg: Int? = null) : ErrorText
}

private fun AppError.errorText(): ErrorText = when (this) {
    AppError.MissingApiKey -> ErrorText.FromResources(Res.string.error_missing_api_key)
    AppError.InvalidApiKey -> ErrorText.FromResources(Res.string.error_invalid_api_key)
    AppError.NoConnection -> ErrorText.FromResources(Res.string.error_no_connection)
    AppError.Timeout -> ErrorText.FromResources(Res.string.error_timeout)
    is AppError.RateLimited -> ErrorText.FromResources(Res.string.error_rate_limited)
    is AppError.AllModelsBusy -> ErrorText.FromResources(Res.string.error_models_busy)
    is AppError.Api -> ErrorText.FromResources(Res.string.error_api, code)
    is AppError.Malformed -> ErrorText.FromResources(Res.string.error_malformed)
    is AppError.NotRecognized -> hint?.takeIf { it.isNotBlank() }?.let(ErrorText::Literal)
        ?: ErrorText.FromResources(Res.string.error_not_recognized)
    is AppError.ImageUnavailable -> ErrorText.FromResources(Res.string.error_image_unavailable)
    AppError.LocationUnavailable -> ErrorText.FromResources(Res.string.error_location_unavailable)
    is AppError.Storage -> ErrorText.FromResources(Res.string.error_storage)
    is AppError.Unexpected -> ErrorText.FromResources(Res.string.error_unexpected)
}

/**
 * Turns a domain [AppError] into something a traveller can act on.
 *
 * Every message answers "what do I do now?" rather than naming the failure: a
 * throttled model becomes "try again in a moment", not "HTTP 503".
 *
 * For anything a composable draws — an inline banner, a card, a retry state. An effect
 * collector must use [userMessage] instead; see the note there for why the difference matters.
 */
@Composable
fun AppError.toUserMessage(): String = when (val text = errorText()) {
    is ErrorText.Literal -> text.value
    is ErrorText.FromResources ->
        text.arg?.let { stringResource(text.id, it) } ?: stringResource(text.id)
}

/**
 * The same message, resolved outside composition.
 *
 * This exists because resolving it *inside* composition and reading the result from an effect
 * collector silently loses the message. The collector runs on the next main-queue turn after
 * `sendEffect`; the recomposition that would have produced the string runs on the next frame,
 * which is later. A route that did
 *
 * ```kotlin
 * val errorMessage = state.error?.toUserMessage()          // ❌ still null when the effect lands
 * CollectEffects(viewModel.effects) { effect ->
 *     is XEffect.ShowMessage -> scope.launch {
 *         snackbarHostState.showError(errorMessage ?: return@launch)
 *     }
 * }
 * ```
 *
 * therefore dropped every first failure on the floor — the journal's failed day summary
 * reached the traveller as a spinner that simply stopped. Resolve from the effect's own
 * payload instead:
 *
 * ```kotlin
 * is JournalEffect.ShowMessage -> scope.launch {
 *     snackbarHostState.showError(effect.error.userMessage())   // ✅ no recomposition involved
 * }
 * ```
 */
suspend fun AppError.userMessage(): String = when (val text = errorText()) {
    is ErrorText.Literal -> text.value
    is ErrorText.FromResources ->
        text.arg?.let { getString(text.id, it) } ?: getString(text.id)
}

/** True when retrying the same action has a real chance of working. */
val AppError.isRetryable: Boolean
    get() = when (this) {
        AppError.NoConnection,
        AppError.Timeout,
        is AppError.RateLimited,
        is AppError.AllModelsBusy,
        is AppError.Api,
        is AppError.Malformed,
        -> true
        else -> false
    }
