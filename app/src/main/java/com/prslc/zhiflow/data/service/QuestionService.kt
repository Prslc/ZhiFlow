package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedResponse
import io.ktor.client.call.body
import io.ktor.client.request.get

suspend fun getQuestionDetail(id: String): QuestionDetail {
    val tag = "questionService"
    val response = Client.client.get("questions/$id")

    if (response.status.value != 200) {
        Log.e(tag, "Request failed with status: ${response.status}")
    }

    return response.body<QuestionDetail>()
}

suspend fun getQuestionFeed(id: String, nextUrl: String? = null): QuestionFeedResponse {
    val tag = "questionFeedService"
    val requestUrl = nextUrl ?: "questions/$id/feeds"

    val response = Client.client.get(requestUrl)

    if (response.status.value != 200) {
        Log.e(tag, "Request failed with status: ${response.status}")
    }

    return response.body<QuestionFeedResponse>()
}