package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.isSuccess

/**
 * Add read history record
 * @param request Read history request body
 */
suspend fun addReadHistory(request: ReadHistoryRequest): Boolean {
    val tag = "addReadHistory"
    return try {
        val response = Client.client.post("read_history/add") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
        val isSuccess = response.status.isSuccess()
        if (isSuccess) {
            Log.d(tag, "Read history synced successfully")
        } else {
            Log.e(tag, "Sync failed with status: ${response.status}")
        }
        isSuccess
    } catch (e: Exception) {
        Log.e(tag, "Network error during sync", e)
        false
    }
}

/**
 * Perform vote action (up/down)
 * @param id target ID
 * @param action Vote type (e.g., "up", "down")
 * @param method "POST" to vote, "DELETE" to remove vote
 */
suspend fun voteAction(
    id: String,
    contentType: ContentType,
    action: String,
    method: String = "POST"
): Boolean {
    val tag = "voteService"
    return try {
        val response = Client.client.request("reaction/${contentType.apiPath}/$id/vote/$action") {
            this.method = io.ktor.http.HttpMethod.parse(method)
        }
        val isSuccess = response.status.isSuccess()
        isSuccess
    } catch (e: Exception) {
        Log.e(tag, "Vote network error: $action ($method)", e)
        false
    }
}