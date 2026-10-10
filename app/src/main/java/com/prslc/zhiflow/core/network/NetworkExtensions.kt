package com.prslc.zhiflow.core.network

import com.prslc.zhiflow.core.exception.ApiException
import com.prslc.zhiflow.core.exception.HttpStatusException
import com.prslc.zhiflow.core.exception.toApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

const val BASE_URL = "https://api.zhihu.com"

/**
 * Extension properties and functions for OkHttp classes to streamline
 * data parsing and resource management.
 */

internal fun Request.Builder.apiUrl(path: String): Request.Builder =
    this.url("${BASE_URL}$path")

/**
 * Sets the URL using [nextUrl] as-is when provided (pagination), otherwise
 * delegates to [apiUrl] to prepend [BASE_URL] to the given [fallbackPath].
 */
internal fun Request.Builder.urlOrApiUrl(nextUrl: String?, fallbackPath: String): Request.Builder =
    if (nextUrl != null) url(nextUrl) else apiUrl(fallbackPath)

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
        if (!res.isSuccessful) throw HttpStatusException(res, res.apiErrorCode())
        HttpClientProvider.jsonInstance.decodeFromString<T>(res.body.string())
    }
}

/**
 * Reads the code a failed response carries in its body.
 *
 * Read here because it has to happen while the response is still open: by the time anyone
 * catches the exception, [use] has closed the body.
 */
private fun Response.apiErrorCode(): Int? = try {
    HttpClientProvider.jsonInstance
        .parseToJsonElement(peekBody(ERROR_BODY_LIMIT).string())
        .jsonObject["error"]
        ?.jsonObject
        ?.get("code")
        ?.jsonPrimitive
        ?.int
} catch (_: Exception) {
    null
}

private const val ERROR_BODY_LIMIT = 4 * 1024L

/**
 * Executes an OkHttp request on [Dispatchers.IO] and parses the response body.
 * Failures (network errors, HTTP errors, parse errors) are caught and mapped
 * to [ApiException] via [toApiException].
 */
internal suspend inline fun <reified T> ApiClient.safeApiCall(
    crossinline requestBuilder: () -> Request
): Result<T> = withContext(Dispatchers.IO) {
    val request = requestBuilder()
    if (!request.establishesSession()) gate.awaitReady()

    var status = 0
    val outcome = try {
        http.newCall(request).execute().let { response ->
            status = response.code
            Result.success(response.body<T>())
        }
    } catch (e: Exception) {
        Result.failure(e.toApiException())
    }

    // A success counts as much as a rejection: the gate renews by watching for the response
    // that finds the session due.
    if (status != 0) gate.onExchange(status)
    outcome
}

/**
 * Executes an OkHttp request on [Dispatchers.IO] when only the outcome matters and the
 * response body is not parsed. Together with [safeApiCall] this helper never reports a
 * rejected request as a success: a non-2xx status is a failure carrying the mapped
 * [ApiException], status code included.
 *
 * The response body is consumed and closed on IO to avoid
 * [android.os.NetworkOnMainThreadException] during cleanup.
 */
internal suspend fun ApiClient.safeExecute(
    requestBuilder: () -> Request
): Result<Unit> = withContext(Dispatchers.IO) {
    val request = requestBuilder()
    if (!request.establishesSession()) gate.awaitReady()
    try {
        http.newCall(request).execute().use { response ->
            gate.onExchange(response.code)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(HttpStatusException(response).toApiException())
            }
        }
    } catch (e: Exception) {
        Result.failure(e.toApiException())
    }
}
