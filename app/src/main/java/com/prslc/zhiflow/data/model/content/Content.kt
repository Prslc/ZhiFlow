package com.prslc.zhiflow.data.model.content

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
interface ZhihuContent {
    val id: String
    val author: AnswerAuthor
    val reaction: Reaction?
    val contentEnd: ContentEndInfo?
    val structuredContent: StructuredContent?

    /** What the content says about being commented on; null where the server said nothing. */
    val commentConfig: CommentConfig?

    val displayTitle: String
}

/**
 * How the content takes comments. [canReply] is true unless the server says otherwise: a payload
 * that omits the block is not one that refuses comments.
 */
@Immutable
@Serializable
data class CommentConfig(
    @SerialName("can_reply") val canReply: Boolean = true,
    @SerialName("can_segment_reply") val canSegmentReply: Boolean = false,
    @SerialName("place_holder") val placeHolder: String? = null,
)
