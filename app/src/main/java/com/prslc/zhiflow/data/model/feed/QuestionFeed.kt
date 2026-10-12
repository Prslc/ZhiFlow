package com.prslc.zhiflow.data.model.feed
import androidx.compose.runtime.Immutable
import com.prslc.zhiflow.data.model.Paging
import com.prslc.zhiflow.data.model.user.ZhihuUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class QuestionFeedResponse(
    val data: List<QuestionFeedItem> = emptyList(),
    val paging: Paging = Paging(),
)

@Immutable
@Serializable
data class QuestionFeedItem(
    val type: String = "", // "question_feed_card"
    @SerialName("target_type") val targetType: String = "", // "answer"
    val target: AnswerTarget = AnswerTarget(),
)

@Immutable
@Serializable
data class AnswerTarget(
    val id: String = "",
    val author: ZhihuUser = ZhihuUser(),
    val excerpt: String = "",
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_time") val createdTime: Long = 0,
    @SerialName("updated_time") val updatedTime: Long = 0,
    @SerialName("thumbnail_info") val thumbnailInfo: ThumbnailInfo? = null,
)

@Immutable
@Serializable
data class ThumbnailInfo(
    val thumbnails: List<ZhihuThumbnail> = emptyList(),
)

@Immutable
@Serializable
data class ZhihuThumbnail(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
)
