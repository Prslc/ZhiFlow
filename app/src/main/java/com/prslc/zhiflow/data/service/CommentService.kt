package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.CommentResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.http.isSuccess

/**
 * Fetch root comments of an answer (supports pagination)
 * @param answerId Answer ID
 * @param offset Pagination offset
 * @param orderBy Sorting method (e.g., "score", "ts")
 * @param limit Number of comments per request
 */
suspend fun getRootComments(
    answerId: String,
    offset: String = "",
    orderBy: String = "score",
    limit: Int = 20
): CommentResponse? {
    val tag = "commentService"
    return try {
        val response = Client.client.get("comment_v5/answers/$answerId/root_comment") {
            parameter("order_by", orderBy)
            parameter("limit", limit)
            parameter("offset", offset)
        }

        if (response.status.value != 200) {
            Log.e(tag, "Request failed with status: ${response.status}")
            return null
        }

        response.body<CommentResponse>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch comments for answer: $answerId", e)
        throw e
    }
}

/**
 * Fetch child comments of a root comment (supports pagination)
 * @param rootCommentId Root comment ID
 * @param offset Pagination offset
 * @param limit Number of comments per request
 */
suspend fun getChildComments(
    rootCommentId: String,
    offset: String = "",
    limit: Int = 20
): CommentResponse? {
    val tag = "commentService"
    return try {
        val response = Client.client.get("comment_v5/comment/$rootCommentId/child_comment") {
            parameter("order_by", "ts")
            parameter("limit", limit)
            parameter("offset", offset)
        }

        if (response.status.value != 200) return null
        response.body<CommentResponse>()
    } catch (e: Exception) {
        Log.e(tag, "Failed to fetch child comments", e)
        null
    }
}

/**
 * Perform comment reaction (like/unlike)
 * @param commentId Comment ID
 * @param action Reaction type (e.g., "like")
 * @param method "POST" to like, "DELETE" to unlike
 */
suspend fun commentReaction(commentId: String, action: String, method: String = "POST"): Boolean {
    val tag = "commentReaction"
    return try {
        val response = Client.client.request("reaction/comments/$commentId/$action") {
            this.method = io.ktor.http.HttpMethod.parse(method)
        }
        val isSuccess = response.status.isSuccess()
        Log.d(tag, "commentReaction $action ($method) on $commentId | Success: $isSuccess")
        isSuccess
    } catch (e: Exception) {
        Log.e(tag, "commentReaction network error: $action ($method)", e)
        false
    }
}