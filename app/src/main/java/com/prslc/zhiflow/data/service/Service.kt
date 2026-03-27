package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuAnswer
import com.prslc.zhiflow.data.model.ZhihuResponse
import com.prslc.zhiflow.data.model.ZhihuUser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

suspend fun getRecommendFeed(limit: Int = 10, nextUrl: String? = null): ZhihuResponse? {
    val TAG = "feedService"
    return try {
        val requestUrl = nextUrl ?: "topstory/recommend"

        val response = Client.client.get(requestUrl) {
            if (nextUrl == null) {
                parameter("limit", limit)
                parameter("action", "down")
            }
        }

        Log.d(TAG, "Requesting: $requestUrl | Status: ${response.status}")

        response.body<ZhihuResponse>()
    } catch (e: Exception) {
        Log.e(TAG, "Network Error", e)
        null
    }
}

suspend fun getAnswerDetail(answerId: String): ZhihuAnswer? {
    val TAG = "answerService"
    return try {
        val response = Client.client.get("answers/v2/$answerId")

        if (response.status.value != 200) {
            Log.e(TAG, "Request failed with status: ${response.status}")
            return null
        }

        response.body<ZhihuAnswer>()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch profile", e)
        throw e
    }
}

suspend fun getUserDetail(): ZhihuUser? {
    val TAG = "userService"
    return try {
        val response = Client.client.get("people/self")
        if (response.status.value != 200) {
            Log.e(TAG, "Request failed with status: ${response.status}")
            return null
        }

        response.body<ZhihuUser>()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch profile", e)
        throw e
    }
}