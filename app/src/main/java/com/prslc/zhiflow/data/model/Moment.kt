package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class MomentsResponse(
    val data: List<ComponentCard> = emptyList(),
    val paging: MomentsPaging = MomentsPaging()
)

@Immutable
@Serializable
data class MomentsPaging(
    @SerialName("is_end") val isEnd: Boolean = false,
    val next: String? = null
)

@Immutable
@Serializable
data class ComponentCard(
    val id: String = "",
    val type: String = "",
    val extra: CardExtra? = null
)

@Immutable
@Serializable
data class CardExtra(
    @SerialName("business_ext_map") val businessExtMap: BusinessExtMap? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("content_id") val contentId: String? = null
)

@Immutable
@Serializable
data class BusinessExtMap(
    @SerialName("content_info") val contentInfo: ContentInfo? = null,
    @SerialName("parent_content_data") val parentContentData: ParentContentData? = null,
    val author: MomentAuthor? = null,
    @SerialName("moments_biz_data") val momentsBizData: MomentsBizData? = null,
    @SerialName("reaction_map") val reactionMap: ReactionMap? = null,
    val router: String? = null
)

@Immutable
@Serializable
data class ContentInfo(
    @SerialName("content_id") val contentId: String? = null,
    @SerialName("content_token") val contentToken: String? = null,
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("published_at") val publishedAt: Long = 0,
    @SerialName("media_detail") val mediaDetail: MediaDetail? = null,
    val detail: ContentDetail? = null
)

@Immutable
@Serializable
data class ContentDetail(
    val title: String? = null,
    @SerialName("plain_content") val plainContent: String? = null,
    val content: String? = null,
    val summary: String? = null
)

@Immutable
@Serializable
data class MediaDetail(
    @SerialName("media_type") val mediaType: String? = null,
    val images: List<MediaImage>? = null,
    val video: MediaVideo? = null,
    val videos: List<MediaVideo>? = null
)

@Immutable
@Serializable
data class MediaImage(
    val url: String? = null,
    val token: String? = null,
    @SerialName("original_token") val originalToken: String? = null,
    val width: Int = 0,
    val height: Int = 0
)

@Immutable
@Serializable
data class MediaVideo(
    @SerialName("video_info") val videoInfo: VideoInfo? = null,
    val thumbnail: String? = null,
    @SerialName("video_id") val videoId: String? = null
)

@Immutable
@Serializable
data class VideoInfo(
    val duration: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    @SerialName("play_count") val playCount: Int = 0,
    val thumbnail: String? = null
)

@Immutable
@Serializable
data class ParentContentData(
    @SerialName("content_info") val contentInfo: ParentContentInfo? = null
)

@Immutable
@Serializable
data class ParentContentInfo(
    @SerialName("content_type") val contentType: String? = null,
    @SerialName("content_id") val contentId: String? = null,
    val detail: ContentDetail? = null
)

@Immutable
@Serializable
data class MomentsBizData(
    @SerialName("action_type") val actionType: String? = null,
    @SerialName("action_text") val actionText: String? = null,
    @SerialName("action_time") val actionTimeMs: Long = 0,
    @SerialName("feed_type") val feedType: String? = null
)

@Immutable
@Serializable
data class MomentAuthor(
    val profile: AuthorProfile? = null,
    val meta: AuthorMeta? = null,
    val badges: List<AuthorBadge>? = null
)

@Immutable
@Serializable
data class AuthorProfile(
    val avatar: AuthorAvatar? = null,
    @SerialName("full_name") val fullName: String? = null
)

@Immutable
@Serializable
data class AuthorAvatar(
    val url: String? = null,
    val token: String? = null
)

@Immutable
@Serializable
data class AuthorMeta(
    val uid: Long = 0,
    @SerialName("url_token") val urlToken: String? = null,
    @SerialName("hash_id") val hashId: String? = null
)

@Immutable
@Serializable
data class AuthorBadge(
    val description: String? = null,
    val type: String? = null,
    val icon: String? = null,
    @SerialName("night_icon") val nightIcon: String? = null
)

@Immutable
@Serializable
data class ReactionMap(
    @SerialName("LIKE") val like: ReactionEntry? = null,
    @SerialName("VOTE_UP") val voteUp: ReactionEntry? = null,
    @SerialName("COLLECT") val collect: ReactionEntry? = null,
    @SerialName("COMMENT") val comment: ReactionEntry? = null,
    @SerialName("SHARE") val share: ReactionEntry? = null,
    @SerialName("SUBSCRIBE") val subscribe: ReactionEntry? = null
)

@Immutable
@Serializable
data class ReactionEntry(
    val count: Int = 0,
    val reacted: Boolean = false,
    @SerialName("reaction_type") val reactionType: String? = null,
    @SerialName("content_id") val contentId: String? = null,
    @SerialName("content_type") val contentType: String? = null
)
