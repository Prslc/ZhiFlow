package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.CommentDto
import com.prslc.zhiflow.data.model.comment.ZhihuComment
import com.prslc.zhiflow.data.remote.parser.CommentParser

internal fun ZhihuComment.toDto(): CommentDto {
    val ipInfo = tags.find { it.type == "ip_info" }?.text
    val parsedContent = CommentParser.parse(content)
    return CommentDto(
        id = id,
        author = author,
        replyToAuthor = replyToAuthor,
        createdTime = createdTime,
        likeCount = likeCount,
        liked = liked,
        childCount = childCount,
        ipInfo = ipInfo,
        parsedContent = parsedContent,
    )
}
