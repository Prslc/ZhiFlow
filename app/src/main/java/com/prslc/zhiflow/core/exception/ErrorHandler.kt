package com.prslc.zhiflow.core.exception

import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** The code the account endpoints answer with when the credential they were given is no good. */
private const val ERROR_INVALID_REFRESH_TOKEN = 100008

// Converters from Throwable and OkHttp Response to domain-specific ApiException.
//
// Since OkHttp does not throw exceptions for non-2xx status codes, we provide an extension for
// Response to handle HTTP errors explicitly.

/**
 * Handles network-level exceptions (e.g., timeouts, no internet).
 *
 * [CancellationException] is re-thrown rather than mapped: cancellation is control flow,
 * not a failure, and mapping it would surface an error state for a request that was merely
 * superseded. This is the single place that decides so.
 */
internal fun Throwable.toApiException(): ApiException {
    if (this is CancellationException) throw this
    return when (this) {
        is HttpStatusException -> {
            response.toApiException(apiErrorCode, apiErrorMessage)
                ?: ApiException.UnknownException()
        }

        is IOException -> {
            ApiException.NetworkException()
        }

        is ApiException -> {
            this
        }

        else -> {
            ApiException.UnknownException()
        }
    }
}

/**
 * Converts an OkHttp [Response] into an [ApiException] if it's not successful.
 * @return The corresponding [ApiException] or null if the response is successful.
 */
private fun Response.toApiException(apiErrorCode: Int?, apiErrorMessage: String?): ApiException? {
    if (isSuccessful) return null

    // A credential the server is finished with does not always arrive as a 401: the account
    // endpoints answer a bad refresh token with 400 and a code of their own. Missing this is
    // silent — the session is simply never given up.
    if (apiErrorCode == ERROR_INVALID_REFRESH_TOKEN) return ApiException.UnAuthorizedException()

    return when (code) {
        401, 403 -> ApiException.UnAuthorizedException()

        404 -> ApiException.NotFoundException()

        in 400..499 -> ApiException.ServerException(code, apiErrorMessage)

        // Client-side but API error
        in 500..599 -> ApiException.ServerException(code, apiErrorMessage)

        // Server-side error
        else -> ApiException.UnknownException()
    }
}
