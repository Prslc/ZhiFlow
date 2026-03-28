package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuResponse
import com.prslc.zhiflow.data.model.ZhihuUser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.isSuccess

suspend fun addReadHistory(request: ReadHistoryRequest): Boolean {
    val tag = "addReadHistory"
    return try {
        val response = Client.client.post("read_history/add") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
        val isSuccess = response.status.isSuccess()
        if (isSuccess) {
            Log.d(tag, "Read history synced successfully (Status: 200)")
        } else {
            Log.e(tag, "Sync failed with status: ${response.status}")
        }
        isSuccess
    } catch (e: Exception) {
        Log.e(tag, "Network error during sync", e)
        false
    }
}

// Performs a vote action (up/down).
// Use method="POST" to vote, and method="DELETE" to remove the vote.
suspend fun voteAction(answerId: String, action: String, method: String = "POST"): Boolean {
    val tag = "voteService"
    return try {
        val response = Client.client.request("reaction/answers/$answerId/vote/$action") {
            this.method = io.ktor.http.HttpMethod.parse(method)
        }
        val isSuccess = response.status.isSuccess()
        Log.d(tag, "Voted $action ($method) on $answerId | Success: $isSuccess")
        isSuccess
    } catch (e: Exception) {
        Log.e(tag, "Vote network error: $action ($method)", e)
        false
    }
}