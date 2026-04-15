package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class QuestionService(private val client: HttpClient) {

    suspend fun getQuestionDetail(id: String): QuestionDetail {
        return client.get("questions/$id").body()
    }

    suspend fun getQuestionFeed(id: String, nextUrl: String? = null): QuestionFeedResponse {
        val requestUrl = nextUrl ?: "questions/$id/feeds"
        return client.get(requestUrl).body()
    }
}