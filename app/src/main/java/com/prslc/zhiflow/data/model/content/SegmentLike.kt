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

    /**
     * The passage the range covers: [text] without the whitespace it trails with. A quote's range
     * runs to the end of its line, so its raw end is a line break rather than a character of the
     * passage, and everything drawn -- underline, bubble, panel -- stops short of it.
     */
    val passageText: String get() = text.trimEnd()

    /** Where [passageText] ends, as an offset into the text the range was marked in. */
    val passageEnd: Int get() = rawEnd - (text.length - passageText.length)
}

@Immutable
@Serializable
data class SegmentReactionResponse(
    @SerialName("seg_id") val segId: String = "",
)
