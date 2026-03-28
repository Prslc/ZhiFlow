package com.prslc.zhiflow.data.service

import android.util.Log
import com.prslc.zhiflow.data.api.Client
import com.prslc.zhiflow.data.model.CommentResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

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