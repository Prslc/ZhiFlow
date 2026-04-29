package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuestionFeedResponse(
    val data: List<QuestionFeedItem>,
    val paging: Paging
)

@Serializable
data class QuestionFeedItem(
    val type: String, // "question_feed_card"
    @SerialName("target_type") val targetType: String, // "answer"
    val target: AnswerTarget
)

@Serializable
data class AnswerTarget(
    val id: String,
    val author: ZhihuUser,
    val excerpt: String,
    @SerialName("voteup_count") val voteupCount: Int,
    @SerialName("comment_count") val commentCount: Int,
    @SerialName("created_time") val createdTime: Long,
    @SerialName("updated_time") val updatedTime: Long,
    @SerialName("thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null
)

@Serializable
data class ThumbnailInfo(
    val thumbnails: List<ZhihuThumbnail> = emptyList()
)

@Immutable
@Serializable
data class ZhihuThumbnail(
    val url: String,
    val width: Int,
    val height: Int
)