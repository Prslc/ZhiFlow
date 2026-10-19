package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.comment.Author
import com.prslc.zhiflow.data.model.comment.AuthorTag
import com.prslc.zhiflow.data.model.comment.CommentAuthor
import com.prslc.zhiflow.data.model.comment.CommentContent

@Immutable
data class CommentDto(
    val id: String,
    val author: CommentAuthor,
    val replyToAuthor: Author?,
    /** The badges the row wears beside the author; see the model. */
    val authorTags: List<AuthorTag>,
    val createdTime: Long,
    val likeCount: Int,
    val liked: Boolean,
    val disliked: Boolean,
    val isAuthorTop: Boolean,
    val childCount: Int,
    /** The replies the response carried, of which the row draws the first; see the mapper. */
    val childComments: List<CommentDto>,
    val ipInfo: String?,
    val parsedContent: CommentContent,
    /** Whether a reply can be left on this comment; false hides the way in. */
    val canReply: Boolean,
    val canLike: Boolean,
    val canDislike: Boolean,
)
