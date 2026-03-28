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
import io.ktor.client.request.setBody
import io.ktor.http.contentType
import io.ktor.http.isSuccess

suspend fun getRecommendFeed(limit: Int = 10, nextUrl: String? = null): ZhihuResponse? {
    val tag = "feedService"
    return try {
        val requestUrl = nextUrl ?: "topstory/recommend"

        val response = Client.client.get(requestUrl) {
            if (nextUrl == null) {
                parameter("limit", limit)
                parameter("action", "down")
            }
        }

        Log.d(tag, "Requesting: $requestUrl | Status: ${response.status}")

        response.body<ZhihuResponse>()
    } catch (e: Exception) {
        Log.e(tag, "Network Error", e)
        null
    }
}

suspend fun getAnswerDetail(answerId: String): ZhihuAnswer? {
    val tag = "answerService"
    return try {
        val response = Client.client.get("answers/v2/$answerId")

        if (response.status.value != 200) {
            Log.e(tag, "Request failed with status: ${response.status}")
            return null
        }

        response.body<ZhihuAnswer>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch profile", e)
        throw e
    }
}

suspend fun getUserDetail(): ZhihuUser? {
    val tag = "userService"
    return try {
        val response = Client.client.get("people/self")
        if (response.status.value != 200) {
            Log.e(tag, "Request failed with status: ${response.status}")
            return null
        }

        response.body<ZhihuUser>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch profile", e)
        throw e
    }
}

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