package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.comment.CommentResponse
import com.prslc.zhiflow.data.model.comment.ZhihuComment
import com.prslc.zhiflow.data.model.content.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service managing top-level and nested child comments for various content types.
 */
class CommentService(private val api: ApiClient) {

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
        limit: Int = 20,
    ): Result<CommentResponse> = api.safeApiCall {
        val url = "${BASE_URL}/comment_v5/${contentType.apiPath}/$id/root_comment"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("order_by", orderBy)
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset)
            .build()

        Request.Builder().url(url).get().build()
    }

    /**
     * Fetches the comments left on one passage of a content item.
     *
     * Same list as [getRootComments], addressed by the segment id a `seg_like` range carries
     * instead of by the content.
     *
     * @param segmentId The range's shared segment id, not the comments' own `resource_id`.
     * @return A [Result] containing [CommentResponse] on success.
     */
    suspend fun getSegmentComments(
        id: String,
        contentType: ContentType,
        segmentId: String,
        offset: String = "",
        orderBy: String = "score",
        limit: Int = 20,
    ): Result<CommentResponse> = api.safeApiCall {
        val url = "${BASE_URL}/comment_v5/${contentType.apiPath}/$id/segment/root_comment"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("segment_id", segmentId)
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
        limit: Int = 20,
    ): Result<CommentResponse> = api.safeApiCall {
        val url = "${BASE_URL}/comment_v5/comment/$rootCommentId/child_comment"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("order_by", "ts")
            .addQueryParameter("limit", limit.toString())
            .addQueryParameter("offset", offset)
            .build()

        Request.Builder().url(url).get().build()
    }

    /**
     * Posts a comment on a content item, or a reply to one of its comments.
     *
     * A reply is the same request with the comment it answers named — the server has no separate
     * path for one — and the `type` in the path is the same word the lists are read under.
     *
     * @param id The content's id.
     * @param contentType What the content is.
     * @param content What to post, as the reader typed it.
     * @param replyToId The comment being answered, or null to comment on the content itself.
     * @return A [Result] holding the comment as the server stored it, which is what the list draws.
     */
    suspend fun postComment(
        id: String,
        contentType: ContentType,
        content: String,
        replyToId: String? = null,
    ): Result<ZhihuComment> = api.safeApiCall {
        val body = HttpClientProvider.jsonInstance
            .encodeToString(CommentRequest(content = content, replyCommentId = replyToId.orEmpty()))
            .toRequestBody(jsonMediaType)

        Request.Builder()
            .apiUrl("/comment_v5/${contentType.apiPath}/$id/comment")
            .post(body)
            .build()
    }

    /**
     * Performs a reaction (like/unlike) on a comment.
     *
     * @param method Standard HTTP method string (e.g., "POST", "DELETE").
     */
    suspend fun commentReaction(commentId: String, action: String, method: String): Result<Unit> =
        api.safeExecute {
            val emptyBody = "".toRequestBody(null)

            Request.Builder()
                .apiUrl("/reaction/comments/$commentId/$action")
                .method(method, if (method == "GET") null else emptyBody)
                .build()
        }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
}

/** What a new comment puts in the fields that name a comment: there is none, and none answered. */
private const val NEW_COMMENT = ""

/**
 * The body a comment is posted with.
 *
 * The server takes more than the words — whether they carry an image, a sticker type, a score —
 * and the rest of them go out as the values a comment written by hand carries; none of them is
 * anything this app has to say. `segment` is among them: a passage's own comment is posted at
 * another path, and this field is null for every comment that is not one.
 */
@Serializable
private data class CommentRequest(
    val content: String,
    @SerialName("comment_id") val commentId: String = NEW_COMMENT,
    @SerialName("reply_comment_id") val replyCommentId: String = NEW_COMMENT,
    val segment: JsonElement? = null,
    @SerialName("has_img") val hasImg: Boolean = false,
    @SerialName("sticker_type") val stickerType: String? = null,
    @SerialName("selected_settings") val selectedSettings: List<String> = emptyList(),
    @SerialName("extra_params") val extraParams: String = "",
    val score: Int = 0,
    @SerialName("unfriendly_check") val unfriendlyCheck: String = "strict",
)
