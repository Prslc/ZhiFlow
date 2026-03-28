package com.prslc.zhiflow.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponse(
    val data: List<ZhihuComment>,
    val counts: CommentCounts,
    val paging: Paging? = null,
    @SerialName("atmosphere_voting_config") val atmosphere: AtmosphereConfig? = null
)

@Serializable
data class ZhihuComment(
    val id: String,
    val content: String,    // html
    @SerialName("created_time") val createdTime: Long,
    @SerialName("like_count") val likeCount: Int,
    val author: CommentAuthor,
    @SerialName("comment_tag") val tags: List<CommentTag> = emptyList(),
    @SerialName("child_comment_count") val childCount: Int = 0,
    val liked: Boolean = false
)

@Serializable
data class CommentCounts(
    @SerialName("total_counts") val total: Int = 0,
    @SerialName("collapsed_counts") val collapsed: Int = 0,
    @SerialName("reviewing_counts") val reviewing: Int = 0,
    @SerialName("segment_comment_counts") val segment: Int = 0
)

@Serializable
data class VipInfo(
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("vip_icon") val vipIcon: VipIcon? = null
)

@Serializable
data class VipIcon(
    val url: String? = null,
    @SerialName("night_mode_url") val nightUrl: String? = null
)

@Serializable
data class ExposedMedal(
    @SerialName("medal_id") val id: String = "",
    @SerialName("medal_name") val name: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
    val description: String = ""
)

@Serializable
data class AtmosphereConfig(
    val title: String? = null,
    val detail: List<AtmosphereEmoji> = emptyList()
)

@Serializable
data class AtmosphereEmoji(
    @SerialName("emoji_level") val level: String = "",
    val title: String = "",
    @SerialName("normal_icon") val icon: String = ""
)

@Serializable
data class CommentAuthor(
    val id: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String,
    val headline: String? = "",
    @SerialName("vip_info") val vipInfo: VipInfo? = null,
    @SerialName("exposed_medal") val medal: ExposedMedal? = null
)

@Serializable
data class CommentTag(
    val type: String,   // ip
    val text: String    // ip address
)