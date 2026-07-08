package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.data.model.comment.CommentResponse
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.remote.service.CommentService

/**
 * Repository responsible for orchestrating comment data from [CommentService].
 * It wraps responses in [Result] and handles domain-specific error mapping.
 */
class CommentRepository(private val service: CommentService) {

    /**
     * Retrieve root comments for a content item.
     *
     * @param id Content ID.
     * @param type Content type (answer/article).
     * @param offset Pagination offset.
     * @return [Result] wrapping [CommentResponse].
     */
    suspend fun getRootComments(
        id: String,
        type: ContentType,
        offset: String = ""
    ): Result<CommentResponse> = service.getRootComments(id, type, offset = offset)

    /**
     * Retrieve replies for a specific comment.
     *
     * @param rootCommentId Root comment ID.
     * @param offset Pagination offset.
     * @return [Result] wrapping [CommentResponse].
     */
    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = ""
    ): Result<CommentResponse> = service.getChildComments(rootCommentId, offset)

    /**
     * Toggle the like status of a comment.
     *
     * @param commentId Target comment ID.
     * @param isLike True to like, false to unlike.
     * @return [Result] wrapping true if the operation succeeded.
     */
    suspend fun toggleLike(commentId: String, isLike: Boolean): Result<Boolean> {
        val method = if (isLike) "POST" else "DELETE"

        return service.commentReaction(
            commentId = commentId,
            action = "like",
            method = method,
        )
    }
}
