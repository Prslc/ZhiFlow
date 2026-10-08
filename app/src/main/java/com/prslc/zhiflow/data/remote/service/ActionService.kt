package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.HttpClientProvider
import com.prslc.zhiflow.core.network.apiUrl
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
import com.prslc.zhiflow.data.model.content.SegmentReactionResponse
import com.prslc.zhiflow.data.model.user.ReadHistoryRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
     * @return A [Result] indicating success or failure.
     */
    suspend fun addReadHistory(request: ReadHistoryRequest): Result<Unit> =
        okHttpClient.safeExecute {
            val jsonBody = HttpClientProvider.jsonInstance.encodeToString(request)
            val body = jsonBody.toRequestBody(jsonMediaType)

            Request.Builder()
                .apiUrl("/read_history/add")
                .post(body)
                .build()
        }

    /**
     * Likes the passage a `seg_like` range covers.
     *
     * The server addresses the range by the shared segment id, the paragraph's `pid` and the raw
     * offsets, and answers with the reader's own segment id — which is the one [unlikeSegment]
     * has to be given.
     *
     * @return A [Result] carrying the reader's segment id.
     */
    suspend fun likeSegment(
        id: String,
        contentType: ContentType,
        target: SegmentLikeTarget,
    ): Result<SegmentReactionResponse> = okHttpClient.safeApiCall {
        val body = HttpClientProvider.jsonInstance.encodeToString(
            SegmentLikeRequest(
                segId = target.segId,
                content = target.text,
                position = SegmentPosition(
                    start = SegmentOffset(target.rawStart, target.paragraphId.orEmpty()),
                    end = SegmentOffset(target.rawEnd, target.paragraphId.orEmpty()),
                ),
            )
        ).toRequestBody(jsonMediaType)

        Request.Builder()
            .apiUrl("/reaction/${contentType.apiPath}/$id/segment_reaction")
            .post(body)
            .build()
    }

    /**
     * Undoes a segment like.
     *
     * @param segId The reader's own segment id, as returned by [likeSegment].
     */
    suspend fun unlikeSegment(
        id: String,
        contentType: ContentType,
        segId: String,
    ): Result<Unit> = okHttpClient.safeExecute {
        val body = HttpClientProvider.jsonInstance
            .encodeToString(SegmentUnlikeRequest(segIds = segId))
            .toRequestBody(jsonMediaType)

        Request.Builder()
            .apiUrl("/reaction/${contentType.apiPath}/$id/segment_reaction")
            .delete(body)
            .build()
    }

    /**
     * Performs a voting action (upvote, downvote, or cancel) on a specific content type.
     *
     * @param id The unique identifier of the content (Answer ID, Article ID, etc.).
     * @param contentType The type of content as defined in [ContentType].
     * @param action The vote action (e.g., "up", "down", "neutral").
     * @param method The HTTP method (POST or DELETE), defaults to POST.
     * @return A [Result] indicating success or failure.
     */
    suspend fun voteAction(
        id: String,
        contentType: ContentType,
        action: String,
        method: String = "POST"
    ): Result<Unit> = okHttpClient.safeExecute {
        val emptyBody = "".toRequestBody(null)

        Request.Builder()
            .apiUrl("/reaction/${contentType.apiPath}/$id/vote/$action")
            .method(method, if (method == "GET") null else emptyBody)
            .build()
    }
}

@Serializable
private data class SegmentLikeRequest(
    @SerialName("seg_id") val segId: String,
    val content: String,
    val position: SegmentPosition,
)

@Serializable
private data class SegmentPosition(
    val start: SegmentOffset,
    val end: SegmentOffset,
)

@Serializable
private data class SegmentOffset(
    val offset: Int,
    @SerialName("paragraph_id") val paragraphId: String,
)

@Serializable
private data class SegmentUnlikeRequest(
    @SerialName("seg_ids") val segIds: String,
)
