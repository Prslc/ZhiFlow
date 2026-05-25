package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.CommentResponse
import com.prslc.zhiflow.data.model.ContentType
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service managing top-level and nested child comments for various content types.
 */
class CommentService(private val okHttpClient: OkHttpClient) {

    /**
     * Fetches root comments for a specific content item.
     *
     * @param id The ID of the content.
     * @param contentType The [ContentType] (e.g., Answer, Article).
     * @param offset The pagination offset string.
     * @param orderBy Sorting order, defaults to "score".
     * @param limit Number of comments to fetch.
     * @return A [Result] containing [CommentResponse] on success.
     */
    suspend fun getRootComments(
        id: String,
        contentType: ContentType,
        offset: String = "",
        orderBy: String = "score",
        limit: Int = 20
    ): Result<CommentResponse> = okHttpClient.safeApiCall {
        val url = "${Client.BASE_URL}/comment_v5/${contentType.apiPath}/$id/root_comment"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("order_by", orderBy)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset)
            .build()

        Request.Builder().url(url).get().build()
    }

    /**
     * Fetches child (replies) for a specific root comment.
     */
    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = "",
        limit: Int = 20
    ): Result<CommentResponse> = okHttpClient.safeApiCall {
        val url = "${Client.BASE_URL}/comment_v5/comment/$rootCommentId/child_comment"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("order_by", "ts")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset)
            .build()

        Request.Builder().url(url).get().build()
    }

    /**
     * Performs a reaction (like/unlike) on a comment.
     *
     * @param method Standard HTTP method string (e.g., "POST", "DELETE").
     */
    suspend fun commentReaction(commentId: String, action: String, method: String): Result<Boolean> =
        okHttpClient.safeExecute {
            val emptyBody = "".toRequestBody(null)

            Request.Builder()
                .apiUrl("/reaction/comments/$commentId/$action")
                .method(method, if (method == "GET") null else emptyBody)
                .build()
        }
}