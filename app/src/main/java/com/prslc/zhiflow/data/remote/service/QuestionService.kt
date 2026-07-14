package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.urlOrApiUrl
import com.prslc.zhiflow.data.model.content.QuestionDetail
import com.prslc.zhiflow.data.model.feed.QuestionFeedResponse
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
            Request.Builder()
                .urlOrApiUrl(nextUrl, "/questions/$id/feeds")
                .get()
                .build()
        }
}
