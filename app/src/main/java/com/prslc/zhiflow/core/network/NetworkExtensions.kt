package com.prslc.zhiflow.core.network

import okhttp3.Request
import okhttp3.Response

/**
 * Extension properties and functions for OkHttp classes to streamline
 * data parsing and resource management.
 */

fun Request.Builder.apiUrl(path: String): Request.Builder =
    this.url("${Client.BASE_URL}$path")

/**
 * Parses the [Response] body into a structured data object of type [T].
 *
 * This function ensures that the [Response] and its underlying [okhttp3.ResponseBody]
 * are closed automatically using [.use] to prevent memory leaks.
 *
 * @param T The expected data model type.
 * @return The deserialized object of type [T], or null if the request was
 * unsuccessful or parsing failed.
 */
inline fun <reified T> Response.body(): T? {
    return use { res ->
        if (!res.isSuccessful) return null
        val string = res.body?.string() ?: return null
        return try {
            Client.jsonInstance.decodeFromString<T>(string)
        } catch (e: Exception) {
            // Log the exception if a logger is available
            null
        }
    }
}