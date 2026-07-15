package com.prslc.zhiflow.data.dto

import com.prslc.zhiflow.data.model.comment.Author
import com.prslc.zhiflow.data.model.comment.CommentAuthor
import com.prslc.zhiflow.data.model.comment.CommentContent

data class CommentDto(
    val id: String,
    val author: CommentAuthor,
    val replyToAuthor: Author?,
    val createdTime: Long,
    val likeCount: Int,
    val liked: Boolean,
    val childCount: Int,
    val ipInfo: String?,
    val parsedContent: CommentContent,
)
