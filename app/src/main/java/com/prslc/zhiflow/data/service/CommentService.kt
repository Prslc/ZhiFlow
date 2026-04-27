package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.core.network.Client
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.body
import com.prslc.zhiflow.data.model.CommentResponse
import com.prslc.zhiflow.data.model.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
     * @return A [CommentResponse] or null on error.
     */
    suspend fun getRootComments(
        id: String,
        contentType: ContentType,
        offset: String = "",
        orderBy: String = "score",
        limit: Int = 20
    ): CommentResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "${Client.BASE_URL}/comment_v5/${contentType.apiPath}/$id/root_comment"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("order_by", orderBy)
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset)
                .build()

            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().body<CommentResponse>()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetches child (replies) for a specific root comment.
     */
    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = "",
        limit: Int = 20
    ): CommentResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "${Client.BASE_URL}/comment_v5/comment/$rootCommentId/child_comment"
                .toHttpUrl()
                .newBuilder()
                .addQueryParameter("order_by", "ts")
                .addQueryParameter("limit", limit.toString())
                .addQueryParameter("offset", offset)
                .build()

            val request = Request.Builder().url(url).get().build()
            okHttpClient.newCall(request).execute().body<CommentResponse>()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Performs a reaction (like/unlike) on a comment.
     *
     * @param method Standard HTTP method string (e.g., "POST", "DELETE").
     */
    suspend fun commentReaction(commentId: String, action: String, method: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val emptyBody = "".toRequestBody(null)

                val request = Request.Builder()
                    .apiUrl("/reaction/comments/$commentId/$action")
                    .method(method, if (method == "GET") null else emptyBody)
                    .build()

                okHttpClient.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
}