package com.prslc.zhiflow.core.exception

import okhttp3.Response
import java.io.IOException

/**
 * Converts a [Throwable] or an OkHttp [Response] into a domain-specific [ApiException].
 *
 * Since OkHttp does not throw exceptions for non-2xx status codes, we provide
 * an extension for [Response] to handle HTTP errors explicitly.
 */

/**
 * Handles network-level exceptions (e.g., timeouts, no internet).
 */
fun Throwable.toApiException(): ApiException {
    return when (this) {
        is IOException -> ApiException.NetworkException()
        is ApiException -> this
        else -> ApiException.UnknownException()
    }
}

/**
 * Converts an OkHttp [Response] into a [ApiException] if it's not successful.
 * * @return The corresponding [ApiException] or null if the response is successful.
 */
fun Response.toApiException(): ApiException? {
    if (isSuccessful) return null

    return when (code) {
        401, 403 -> ApiException.UnAuthorizedException()
        404 -> ApiException.NotFoundException()
        in 400..499 -> ApiException.ServerException(code) // Client-side but API error
        in 500..599 -> ApiException.ServerException(code) // Server-side error
        else -> ApiException.UnknownException()
    }
}