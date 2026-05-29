package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.HttpStatusException
import com.prslc.zhiflow.core.exception.toApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Extension properties and functions for OkHttp classes to streamline
 * data parsing and resource management.
 */

internal fun Request.Builder.apiUrl(path: String): Request.Builder =
    this.url("${Client.BASE_URL}$path")

/**
 * Parses the [Response] body into a structured data object of type [T].
 *
 * This function ensures that the [Response] and its underlying [okhttp3.ResponseBody]
 * are closed automatically using [.use] to prevent memory leaks.
 *
 * Throws [HttpStatusException] on non-2xx responses, [IOException] on empty body,
 * and propagates JSON parsing exceptions.
 *
 * @param T The expected data model type.
 * @return The deserialized object of type [T].
 */
internal inline fun <reified T> Response.body(): T {
    return use { res ->
        if (!res.isSuccessful) throw HttpStatusException(res)
        Client.jsonInstance.decodeFromString<T>(res.body.string())
    }
}

/**
 * Executes an OkHttp request on [Dispatchers.IO] and parses the response body.
 * Failures (network errors, HTTP errors, parse errors) are caught and mapped
 * to [ApiException] via [toApiException].
 */
internal suspend inline fun <reified T> OkHttpClient.safeApiCall(
    crossinline requestBuilder: () -> Request
): Result<T> = withContext(Dispatchers.IO) {
    try {
        Result.success(newCall(requestBuilder()).execute().body<T>())
    } catch (e: Exception) {
        Result.failure(e.toApiException())
    }
}

/**
 * Executes an OkHttp request on [Dispatchers.IO] and returns whether the
 * response was successful. The response body is consumed and closed on IO
 * to avoid [android.os.NetworkOnMainThreadException] during cleanup.
 */
internal suspend fun OkHttpClient.safeExecute(
    requestBuilder: () -> Request
): Result<Boolean> = withContext(Dispatchers.IO) {
    try {
        newCall(requestBuilder()).execute().use { response ->
            Result.success(response.isSuccessful)
        }
    } catch (e: Exception) {
        Result.failure(e.toApiException())
    }
}