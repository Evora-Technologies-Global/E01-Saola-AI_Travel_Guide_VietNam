package com.duylt.trave.vietlensai.core.util

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import com.duylt.trave.vietlensai.resources.Res
import com.duylt.trave.vietlensai.resources.error_api
import com.duylt.trave.vietlensai.resources.error_image_unavailable
import com.duylt.trave.vietlensai.resources.error_invalid_api_key
import com.duylt.trave.vietlensai.resources.error_location_unavailable
import com.duylt.trave.vietlensai.resources.error_malformed
import com.duylt.trave.vietlensai.resources.error_missing_api_key
import com.duylt.trave.vietlensai.resources.error_models_busy
import com.duylt.trave.vietlensai.resources.error_no_connection
import com.duylt.trave.vietlensai.resources.error_not_recognized
import com.duylt.trave.vietlensai.resources.error_rate_limited
import com.duylt.trave.vietlensai.resources.error_storage
import com.duylt.trave.vietlensai.resources.error_timeout
import com.duylt.trave.vietlensai.resources.error_unexpected
import com.duylt.trave.vietlensai.domain.util.AppError

/**
 * Turns a domain [AppError] into something a traveller can act on.
 *
 * Every message answers "what do I do now?" rather than naming the failure: a
 * throttled model becomes "try again in a moment", not "HTTP 503".
 */
@Composable
fun AppError.toUserMessage(): String = when (this) {
    AppError.MissingApiKey -> stringResource(Res.string.error_missing_api_key)
    AppError.InvalidApiKey -> stringResource(Res.string.error_invalid_api_key)
    AppError.NoConnection -> stringResource(Res.string.error_no_connection)
    AppError.Timeout -> stringResource(Res.string.error_timeout)
    is AppError.RateLimited -> stringResource(Res.string.error_rate_limited)
    is AppError.AllModelsBusy -> stringResource(Res.string.error_models_busy)
    is AppError.Api -> stringResource(Res.string.error_api, code)
    is AppError.Malformed -> stringResource(Res.string.error_malformed)
    is AppError.NotRecognized -> hint?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.error_not_recognized)
    is AppError.ImageUnavailable -> stringResource(Res.string.error_image_unavailable)
    AppError.LocationUnavailable -> stringResource(Res.string.error_location_unavailable)
    is AppError.Storage -> stringResource(Res.string.error_storage)
    is AppError.Unexpected -> stringResource(Res.string.error_unexpected)
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
