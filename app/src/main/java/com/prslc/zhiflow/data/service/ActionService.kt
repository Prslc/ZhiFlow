package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.model.ReadHistoryRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service for handling user interactions such as voting and history tracking.
 */
class ActionService(private val okHttpClient: OkHttpClient) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Adds a content item to the user's read history.
     *
     * @param request The history data to be sent.
     * @return True if the server responded with a 2xx status code.
     */
    suspend fun addReadHistory(request: ReadHistoryRequest): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonBody = Client.jsonInstance.encodeToString(request)
            val body = jsonBody.toRequestBody(jsonMediaType)

            val httpRequest = Request.Builder()
                .apiUrl("/read_history/add")
                .post(body)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Performs a voting action (upvote, downvote, or cancel) on a specific content type.
     *
     * @param id The unique identifier of the content (Answer ID, Article ID, etc.).
     * @param contentType The type of content as defined in [ContentType].
     * @param action The vote action (e.g., "up", "down", "neutral").
     * @param method The HTTP method (POST or DELETE), defaults to POST.
     * @return True if the action succeeded.
     */
    suspend fun voteAction(
        id: String,
        contentType: ContentType,
        action: String,
        method: String = "POST"
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody(null)

            val httpRequest = Request.Builder()
                .apiUrl("/reaction/${contentType.apiPath}/$id/vote/$action")
                .method(method, if (method == "GET") null else emptyBody)
                .build()

            val response = okHttpClient.newCall(httpRequest).execute()
            response.use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }
}