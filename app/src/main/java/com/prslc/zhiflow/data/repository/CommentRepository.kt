package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.model.comment.CommentResponse
import com.prslc.zhiflow.data.model.content.ContentType
import com.prslc.zhiflow.data.model.content.SegmentLikeTarget
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
        offset: String = "",
    ): Result<CommentResponse> = service.getRootComments(id, type, offset = offset)

    /**
     * Retrieve the comments left on one passage of a content item.
     *
     * @param segmentId The range's shared segment id, as carried by its `seg_like` mark.
     * @param offset Pagination offset.
     * @return [Result] wrapping [CommentResponse].
     */
    suspend fun getSegmentComments(
        id: String,
        type: ContentType,
        segmentId: String,
        offset: String = "",
    ): Result<CommentResponse> = service.getSegmentComments(id, type, segmentId, offset)

    /**
     * Retrieve replies for a specific comment.
     *
     * @param rootCommentId Root comment ID.
     * @param offset Pagination offset.
     * @return [Result] wrapping [CommentResponse].
     */
    suspend fun getChildComments(
        rootCommentId: String,
        offset: String = "",
    ): Result<CommentResponse> = service.getChildComments(rootCommentId, offset)

    /**
     * Post a comment, or a reply to one.
     *
     * @param id Content ID.
     * @param type Content type. The same word the lists are read under.
     * @param content What to post.
     * @param replyToId The comment being answered; null comments on the content itself.
     * @return [Result] wrapping the comment as the server stored it.
     */
    suspend fun postComment(
        id: String,
        type: ContentType,
        content: String,
        replyToId: String? = null,
    ): Result<CommentDto> = service.postComment(id, type, content, replyToId)
        .mapOffMain { it.toDto() }

    /**
     * Post a comment on one passage of a content item, or a reply to one of those.
     *
     * @param id Content ID.
     * @param type Content type.
     * @param content What to post.
     * @param anchor The passage the comment is filed under.
     * @param replyToId The comment being answered; null leaves a new one on the passage.
     * @return [Result] wrapping the comment as the server stored it.
     */
    suspend fun postSegmentComment(
        id: String,
        type: ContentType,
        content: String,
        anchor: SegmentLikeTarget,
        replyToId: String? = null,
    ): Result<CommentDto> = service.postSegmentComment(id, type, content, anchor, replyToId)
        .mapOffMain { it.toDto() }

    /**
     * Toggle the like status of a comment.
     *
     * @param commentId Target comment ID.
     * @param isLike True to like, false to unlike.
     * @return [Result] wrapping true if the operation succeeded.
     */
    suspend fun toggleLike(commentId: String, isLike: Boolean): Result<Unit> {
        val method = if (isLike) "POST" else "DELETE"

        return service.commentReaction(
            commentId = commentId,
            action = "like",
            method = method,
        )
    }
}
