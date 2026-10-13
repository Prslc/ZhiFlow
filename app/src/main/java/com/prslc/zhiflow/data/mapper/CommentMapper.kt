package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.model.comment.ZhihuComment
import com.prslc.zhiflow.data.remote.parser.CommentParser

/**
 * The comment as its row draws it, with the replies the response carried under it.
 *
 * One level of them: a reply comes in the same shape as a root and can carry replies of its own, so
 * mapping those as well would walk as deep as the payload happens to nest. The pane is what fetches
 * below this row, so one level is as far as the row reads.
 */
internal fun ZhihuComment.toDto(): CommentDto =
    toDtoWith(childComments = childComments.map { it.toDtoWith(childComments = emptyList()) })

/**
 * The same, with the replies already mapped by the caller -- which is what bounds [toDto]'s depth.
 */
private fun ZhihuComment.toDtoWith(childComments: List<CommentDto>): CommentDto {
    val ipInfo = tags.find { it.type == "ip_info" }?.text
    val parsedContent = CommentParser.parse(content)
    return CommentDto(
        id = id,
        author = author,
        replyToAuthor = replyToAuthor,
        createdTime = createdTime,
        likeCount = likeCount,
        liked = liked,
        isAuthor = isAuthor,
        isAuthorTop = isAuthorTop,
        childCount = childCount,
        childComments = childComments,
        ipInfo = ipInfo,
        parsedContent = parsedContent,
        canReply = canReply,
    )
}
