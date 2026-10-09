package com.prslc.zhiflow.core.exception

import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

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
        is HttpStatusException -> response.toApiException() ?: ApiException.UnknownException()
        is IOException -> ApiException.NetworkException()
        is ApiException -> this
        else -> ApiException.UnknownException()
    }
}

/**
 * Converts an OkHttp [Response] into an [ApiException] if it's not successful.
 * @return The corresponding [ApiException] or null if the response is successful.
 */
private fun Response.toApiException(): ApiException? {
    if (isSuccessful) return null

    return when (code) {
        401, 403 -> ApiException.UnAuthorizedException()
        404 -> ApiException.NotFoundException()
        in 400..499 -> ApiException.ServerException(code) // Client-side but API error
        in 500..599 -> ApiException.ServerException(code) // Server-side error
        else -> ApiException.UnknownException()
    }
}
