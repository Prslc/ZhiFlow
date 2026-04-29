package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZhihuAnswer(
    override val id: String,
    override val author: AnswerAuthor,
    val header: Header? = null,
    val question: ZhihuQuestion? = null,
    override val reaction: Reaction,
    @SerialName("content_end_info") override val contentEnd: ContentEndInfo? = null,
    @SerialName("structured_content") override val structuredContent: StructuredContent
) : ZhihuContent {
    override val displayTitle: String
        get() = question?.title ?: "No title"
}

@Immutable
@Serializable
data class AnswerAuthor(
    val fullname: String = "",
    @SerialName("url_token")
    val urlToken: String = "",
    val description: String = "",
    val id: String = "",
    val name: String = "",
    val headline: String = "",
    @SerialName("avatar_url")
    val avatarUrl: String = "",
    val avatar: AvatarContainer? = null
)

@Immutable
@Serializable
data class AvatarContainer(
    @SerialName("avatar_image") val avatarImage: AvatarImage? = null
)

@Immutable
@Serializable
data class AvatarImage(
    val day: String? = null,
    val night: String? = null,
    val width: Int = 0,
    val height: Int = 0
)

@Immutable
@Serializable
data class Header(
    val text: String, // question title
    @SerialName("sub_title") val subTitle: SubTitle? = null
)

@Immutable
@Serializable
data class SubTitle(val text: String)

@Immutable
@Serializable
data class Reaction(
    val statistics: Statistics,
    val relation: Relation? = null
)

@Immutable
@Serializable
data class Relation(
    val vote: String? = "NEUTRAL",
    val faved: Boolean = false
)

@Immutable
@Serializable
data class Statistics(
    @SerialName("favorites") val favoritesCount: Int,
    @SerialName("up_vote_count") val upVoteCount: Int,
    @SerialName("comment_count") val commentCount: Int,
)

@Immutable
@Serializable
data class ContentEndInfo(
    @SerialName("ip_info") val ipInfo: String,
    @SerialName("create_time_text") val createTime: String? = null,
    @SerialName("update_time_text") val updateTime: String? = null
)

@Stable
@Serializable
data class ZhihuImage(
    val urls: List<String>,
    val width: Int,
    val height: Int,
    val description: String,
    @SerialName("is_gif") val isGif: Boolean,
)

@Immutable
@Serializable
data class ZhihuQuestion(
    val id: String,
    val title: String,
    val type: String = "question"
)