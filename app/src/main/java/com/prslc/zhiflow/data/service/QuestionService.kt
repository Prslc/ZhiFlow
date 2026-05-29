package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.data.model.QuestionDetail
import com.prslc.zhiflow.data.model.QuestionFeedResponse
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
     * @return A [Result] containing [QuestionDetail] on success.
     */
    suspend fun getQuestionDetail(id: String): Result<QuestionDetail> =
        okHttpClient.safeApiCall {
            Request.Builder()
                .apiUrl("/questions/$id")
                .get()
                .build()
        }

    /**
     * Fetches the answers/feeds for a specific question.
     *
     * @param id The unique identifier for the question.
     * @param nextUrl The pagination URL for the next set of answers.
     * @return A [Result] containing [QuestionFeedResponse] on success.
     */
    suspend fun getQuestionFeed(id: String, nextUrl: String? = null): Result<QuestionFeedResponse> =
        okHttpClient.safeApiCall {
            // If nextUrl is provided, use it directly; otherwise, construct the initial URL
            val requestUrl = nextUrl ?: "${Client.BASE_URL}/questions/$id/feeds"

            Request.Builder()
                .url(requestUrl)
                .get()
                .build()
        }
}
