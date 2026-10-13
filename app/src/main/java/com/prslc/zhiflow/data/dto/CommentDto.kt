package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.comment.Author
import com.prslc.zhiflow.data.model.comment.CommentAuthor
import com.prslc.zhiflow.data.model.comment.CommentContent

@Immutable
data class CommentDto(
    val id: String,
    val author: CommentAuthor,
    val replyToAuthor: Author?,
    val createdTime: Long,
    val likeCount: Int,
    val liked: Boolean,
    val isAuthor: Boolean,
    val isAuthorTop: Boolean,
    val childCount: Int,
    val ipInfo: String?,
    val parsedContent: CommentContent,
    /** Whether a reply can be left on this comment; false hides the way in. */
    val canReply: Boolean,
)
