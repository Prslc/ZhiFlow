package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.CommentResponse
import com.prslc.zhiflow.data.model.ContentType
import com.prslc.zhiflow.data.service.CommentService
import io.ktor.http.HttpMethod

class CommentRepository(private val service: CommentService) {

    /**
     * Retrieve root comments for a content item
     * @param id Content ID
     * @param type Content type (answer/article)
     * @param offset Pagination offset
     * @return [Result] wrapping [CommentResponse]
     */
    suspend fun getRootComments(
        id: String,
        type: ContentType,
        offset: String = ""
    ): Result<CommentResponse> {
        return try {
            val response = service.getRootComments(id, type, offset = offset)
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to fetch comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retrieve replies for a specific comment
     * @param rootCommentId Root comment ID
     * @param offset Pagination offset
     * @return [Result] wrapping [CommentResponse]
     */
    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = ""
    ): Result<CommentResponse> {
        return try {
            val response = service.getChildComments(rootCommentId, offset)
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(Exception("Failed to fetch replies"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggle the like status of a comment
     * @param commentId Target comment ID
     * @param isLike True to like, false to unlike
     * @return True if operation succeeded
     */
    suspend fun toggleLike(commentId: String, isLike: Boolean): Boolean {
        val method = if (isLike) HttpMethod.Post else HttpMethod.Delete
        return service.commentReaction(
            commentId = commentId,
            action = "like",
            method = method
        )
    }
}