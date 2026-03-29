package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.CommentResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.http.isSuccess

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

// Performs a like comment action (like/unlike).
// Use method="POST" to reaction, and method="DELETE" to remove the reaction.
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