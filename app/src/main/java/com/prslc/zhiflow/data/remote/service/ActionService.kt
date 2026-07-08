package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
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
     * @return A [Result] containing true if the server responded with a 2xx status code.
     */
    suspend fun addReadHistory(request: ReadHistoryRequest): Result<Boolean> =
        okHttpClient.safeExecute {
            val jsonBody = Client.jsonInstance.encodeToString(request)
            val body = jsonBody.toRequestBody(jsonMediaType)

            Request.Builder()
                .apiUrl("/read_history/add")
                .post(body)
                .build()
        }

    /**
     * Performs a voting action (upvote, downvote, or cancel) on a specific content type.
     *
     * @param id The unique identifier of the content (Answer ID, Article ID, etc.).
     * @param contentType The type of content as defined in [ContentType].
     * @param action The vote action (e.g., "up", "down", "neutral").
     * @param method The HTTP method (POST or DELETE), defaults to POST.
     * @return A [Result] containing true if the action succeeded.
     */
    suspend fun voteAction(
        id: String,
        contentType: ContentType,
        action: String,
        method: String = "POST"
    ): Result<Boolean> = okHttpClient.safeExecute {
        val emptyBody = "".toRequestBody(null)

        Request.Builder()
            .apiUrl("/reaction/${contentType.apiPath}/$id/vote/$action")
            .method(method, if (method == "GET") null else emptyBody)
            .build()
    }
}
