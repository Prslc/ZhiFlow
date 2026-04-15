package com.prslc.zhiflow.data.service

import com.prslc.zhiflow.data.model.CommentResponse
import com.prslc.zhiflow.data.model.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import io.ktor.http.isSuccess

class CommentService(private val client: HttpClient) {

    suspend fun getRootComments(
        id: String,
        contentType: ContentType,
        offset: String = "",
        orderBy: String = "score",
        limit: Int = 20
    ): CommentResponse? {
        return try {
            val response = client.get("comment_v5/${contentType.apiPath}/$id/root_comment") {
                parameter("order_by", orderBy)
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = "",
        limit: Int = 20
    ): CommentResponse? {
        return try {
            val response = client.get("comment_v5/comment/$rootCommentId/child_comment") {
                parameter("order_by", "ts")
                parameter("limit", limit)
                parameter("offset", offset)
            }
            if (response.status.isSuccess()) response.body() else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun commentReaction(commentId: String, action: String, method: HttpMethod): Boolean {
        return try {
            val response = client.request("reaction/comments/$commentId/$action") {
                this.method = method
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}