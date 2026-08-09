package com.prslc.zhiflow.data.model.content

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
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
    @SerialName("structured_content") override val structuredContent: StructuredContent? = null,

    @SerialName("video_info") val videoInfo: String? = null,
    @SerialName("image_list") val imageList: PinImageList? = null,

    @SerialName("comment_config") val commentConfig: PinCommentConfig? = null,

    @SerialName("relationship_tips") val relationshipTips: RelationshipTips? = null
) : ZhihuContent {
    override val displayTitle: String
        get() = header?.text ?: excerpt?.take(20) ?: "Pin Content"
}

@Immutable
@Serializable
data class PinImageList(
    val count: Int = 0,
    val images: List<PinImage> = emptyList(),
    @SerialName("is_grid") val isGrid: Boolean = false,
    @SerialName("width_ratio") val widthRatio: Double = 0.0,
)

@Immutable
@Serializable
data class PinImage(
    val token: String? = null,
    val url: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("original_token") val originalToken: String? = null,
    val thumbnail: String? = null,
    val suffix: String? = null,
    @SerialName("original_url") val originalUrl: String? = null,
    @SerialName("original_width") val originalWidth: Int = 0,
    @SerialName("original_height") val originalHeight: Int = 0,
    @SerialName("original_aspect_radio") val originalAspectRatio: Double = 0.0,
    @SerialName("original_scale_ratio") val originalScaleRatio: Double = 0.0,
)

@Immutable
@Serializable
data class PinHeader(
    @SerialName("text") val text: String = "",
    @SerialName("action_url") val actionUrl: String? = null,
    @SerialName("text_size") val textSize: Int = 17,
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("bold") val bold: Boolean = false,
)

@Immutable
@Serializable
data class PinCommentConfig(
    @SerialName("place_holder") val placeHolder: String? = null,
    @SerialName("can_reply") val canReply: Boolean = false,
    @SerialName("can_segment_reply") val canSegmentReply: Boolean = false,
)

@Immutable
@Serializable
data class RelationshipTips(
    val text: String = "",
    @SerialName("text_color") val textColor: String? = null,
    @SerialName("action_url") val actionUrl: String? = null,
)
