package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuPin(
    override val id: String,
    val type: String = "pin",
    @SerialName("excerpt") val excerpt: String? = null,
    @SerialName("is_mine") val isMine: Boolean = false,
    @SerialName("can_copy") val canCopy: Boolean = true,
    @SerialName("is_topping") val isTopping: Boolean = false,

    @SerialName("header") val header: PinHeader? = null,

    override val author: AnswerAuthor,
    override val reaction: Reaction,
    @SerialName("content_end_info") override val contentEnd: ContentEndInfo? = null,
    @SerialName("structured_content") override val structuredContent: StructuredContent,

    @SerialName("video_info") val videoInfo: String? = null,
    @SerialName("image_list") val imageList: List<String>? = null,

    @SerialName("comment_config") val commentConfig: PinCommentConfig? = null,

    @SerialName("relationship_tips") val relationshipTips: RelationshipTips? = null
) : ZhihuContent {
    override val displayTitle: String
        get() = header?.text ?: excerpt?.take(20) ?: "Pin Content"
}

@Serializable
data class PinHeader(
    @SerialName("text") val text: String = "",
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("text_size") val textSize: Int = 17,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("bold") val bold: Boolean = false
)

@Serializable
data class PinCommentConfig(
    @SerialName("place_holder") val placeHolder: String? = null,
    @SerialName("can_reply") val canReply: Boolean = false,
    @SerialName("can_segment_reply") val canSegmentReply: Boolean = false
)

@Serializable
data class RelationshipTips(
    val text: String = "",
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("action_url") val actionUrl: String? = null
)