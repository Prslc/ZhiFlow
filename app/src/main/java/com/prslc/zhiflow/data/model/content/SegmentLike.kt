package com.prslc.zhiflow.data.model.content

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class SegmentLike(
    @SerialName("seg_ids") val segIds: List<String> = emptyList(),
    val count: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("my_comment_count") val myCommentCount: Int = 0,
    @SerialName("is_like") val isLike: Boolean = false,
    @SerialName("is_span") val isSpan: Boolean = false,
)

@Immutable
@Serializable
data class SegmentLikeTarget(
    val segId: String,
    val paragraphId: String? = null,
    val rawStart: Int = 0,
    val rawEnd: Int = 0,
    val text: String = "",
    val mySegId: String? = null,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
) {
    val key: String get() = "$paragraphId:$rawStart:$rawEnd"
}

@Immutable
@Serializable
data class SegmentReactionResponse(
    @SerialName("seg_id") val segId: String = "",
)
