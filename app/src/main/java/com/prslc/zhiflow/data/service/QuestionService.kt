package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Service for fetching question details and their associated answer feeds.
 */
class QuestionService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches detailed information about a specific question.
     *
     * @param id The unique identifier for the question.
     * @return A [QuestionDetail] object containing title, description, etc.
     */
    suspend fun getQuestionDetail(id: String): QuestionDetail? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.zhihu.com/questions/$id")
                .get()
                .build()

            okHttpClient.newCall(request).execute().body<QuestionDetail>()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetches the answers/feeds for a specific question.
     *
     * @param id The unique identifier for the question.
     * @param nextUrl The pagination URL for the next set of answers.
     * @return A [QuestionFeedResponse] containing the list of answers.
     */
    suspend fun getQuestionFeed(id: String, nextUrl: String? = null): QuestionFeedResponse? =
        withContext(Dispatchers.IO) {
            try {
                // If nextUrl is provided, use it directly; otherwise, construct the initial URL
                val requestUrl = nextUrl ?: "https://api.zhihu.com/questions/$id/feeds"

                val request = Request.Builder()
                    .url(requestUrl)
                    .get()
                    .build()

                okHttpClient.newCall(request).execute().body<QuestionFeedResponse>()
            } catch (e: Exception) {
                null
            }
        }
}