package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.ZhihuAnswer
import io.ktor.client.call.body
import io.ktor.client.request.get

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