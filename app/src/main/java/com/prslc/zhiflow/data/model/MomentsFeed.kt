package com.prslc.zhiflow.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Immutable
@Serializable
data class MomentsFeedResponse(
    override val data: List<MomentsFeedItem> = emptyList(),
    override val paging: MomentsPaging = MomentsPaging(),
) : MomentsPage<MomentsFeedItem>

@Immutable
@Serializable
data class MomentsFeedItem(
    val id: String = "",
    val type: String = "",
    @SerialName("attached_info") val attachedInfo: String? = null,
    val brief: String? = null,
    val blocks: List<JsonElement> = emptyList(),
    val interaction: MomentsInteraction? = null,
    @SerialName("moment_desc") val momentDesc: String? = null,
    val score: Long = 0,
    val source: MomentsSource? = null,
    val target: MomentsTarget? = null,
    @SerialName("target_desc") val targetDesc: String? = null,
    @SerialName("target_type") val targetType: String? = null,
    val extra: CardExtra? = null,
)

@Immutable
@Serializable
data class MomentsInteraction(
    @SerialName("can_cancel_top") val canCancelTop: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_recent_top") val canRecentTop: Boolean = false,
    @SerialName("can_share") val canShare: Boolean = false,
    @SerialName("can_show_other_activity") val canShowOtherActivity: Boolean = false,
    @SerialName("can_top") val canTop: Boolean = false,
)

@Immutable
@Serializable
data class MomentsSource(
    @SerialName("action_text") val actionText: String? = null,
    @SerialName("action_time") val actionTime: Long = 0,
    @SerialName("action_type") val actionType: String? = null,
    val actor: MomentsUser? = null,
    val description: String? = null,
    val style: String? = null,
)

@Immutable
@Serializable
data class MomentsTarget(
    val type: String? = null,
    val title: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val name: String? = null,
    val headline: String? = null,
    val description: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    val voting: Int = 0,
    val author: MomentsUser? = null,
    val id: @Serializable(with = StringOrLongSerializer::class) String? = null,
    val url: String? = null,
    val excerpt: String? = null,
    val thumbnail: String? = null,
    @SerialName("excerpt_title") val excerptTitle: String? = null,
    @SerialName("voteup_count") val voteupCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("favlists_count") val favlistsCount: Int = 0,
    @SerialName("reaction_count") val reactionCount: Int = 0,
    @SerialName("repin_count") val repinCount: Int = 0,
    @SerialName("content_mark") val contentMark: JsonObject? = null,
    val content: List<MomentsPinContent>? = null,
    val question: MomentsQuestion? = null,
    @SerialName("label_info") val labelInfo: MomentsLabelInfo? = null,
    @SerialName("lego_info") val legoInfo: MomentsLegoInfo? = null,
    val relationship: MomentsRelationship? = null,
    @SerialName("reaction_instruction") val reactionInstruction: JsonObject? = null,
    @SerialName("reaction_relation") val reactionRelation: MomentsPinReactionRelation? = null,
    @SerialName("origin_pin") val originPin: JsonObject? = null,
    val virtuals: MomentsPinVirtuals? = null,
    @SerialName("regulate_info") val regulateInfo: MomentsRegulateInfo? = null,
    @SerialName("reviewing_info") val reviewingInfo: MomentsReviewingInfo? = null,
    val location: JsonObject? = null,
    val tags: List<JsonElement> = emptyList(),
    val topics: List<JsonElement> = emptyList(),
    val questions: List<JsonElement> = emptyList(),
)

@Immutable
@Serializable
data class MomentsUser(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val badge: List<MomentsBadge> = emptyList(),
    @SerialName("badge_v2") val badgeV2: MomentsBadgeV2? = null,
    val description: String? = null,
    @SerialName("exposed_medal") val exposedMedal: MomentsExposedMedal? = null,
    val gender: Int = 0,
    val headline: String? = null,
    val id: String? = null,
    @SerialName("is_followed") val isFollowed: Boolean = false,
    @SerialName("is_following") val isFollowing: Boolean = false,
    @SerialName("is_org") val isOrg: Boolean = false,
    val name: String? = null,
    val type: String? = null,
    val url: String? = null,
    @SerialName("url_token") val urlToken: String? = null,
    @SerialName("user_status") val userStatus: JsonObject? = null,
    @SerialName("user_type") val userType: String? = null,
    @SerialName("kvip_info") val kvipInfo: MomentsVipInfo? = null,
    @SerialName("vip_info") val vipInfo: MomentsVipInfo? = null,
)

@Immutable
@Serializable
data class MomentsBadge(
    val description: String? = null,
    @SerialName("topic_names") val topicNames: List<JsonElement> = emptyList(),
    val topics: List<JsonElement> = emptyList(),
    val type: String? = null,
)

@Immutable
@Serializable
data class MomentsBadgeV2(
    @SerialName("detail_badges") val detailBadges: List<MomentsDetailBadge> = emptyList(),
    val icon: String? = null,
    @SerialName("merged_badges") val mergedBadges: List<MomentsMergedBadge> = emptyList(),
    @SerialName("night_icon") val nightIcon: String? = null,
    val title: String? = null,
)

@Immutable
@Serializable
data class MomentsDetailBadge(
    @SerialName("badge_status") val badgeStatus: String? = null,
    val description: String? = null,
    @SerialName("detail_type") val detailType: String? = null,
    val icon: String? = null,
    @SerialName("night_icon") val nightIcon: String? = null,
    val sources: List<MomentsBadgeSource> = emptyList(),
    val title: String? = null,
    val type: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsBadgeSource(
    @SerialName("avatar_path") val avatarPath: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val description: String? = null,
    val id: String? = null,
    val name: String? = null,
    val priority: Int = 0,
    val token: String? = null,
    val type: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsMergedBadge(
    @SerialName("badge_status") val badgeStatus: String? = null,
    val description: String? = null,
    @SerialName("detail_type") val detailType: String? = null,
    val icon: String? = null,
    @SerialName("night_icon") val nightIcon: String? = null,
    val sources: List<MomentsBadgeSource>? = null,
    val title: String? = null,
    val type: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsVipInfo(
    @SerialName("is_vip") val isVip: Boolean = false,
    @SerialName("target_url") val targetUrl: String? = null,
    @SerialName("vip_icon") val vipIcon: MomentsVipIcon? = null,
    val widget: MomentsVipWidget? = null,
)

@Immutable
@Serializable
data class MomentsVipIcon(
    @SerialName("night_mode_url") val nightModeUrl: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsVipWidget(
    val id: Int = 0,
    @SerialName("night_mode_url") val nightModeUrl: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsQuestion(
    @SerialName("answer_count") val answerCount: Int = 0,
    val excerpt: String? = null,
    @SerialName("follower_count") val followerCount: Int = 0,
    val id: String? = null,
    @SerialName("is_following") val isFollowing: Boolean = false,
    val name: String? = null,
    @SerialName("reaction_instruction") val reactionInstruction: JsonObject? = null,
    val title: String? = null,
    val type: String? = null,
    val url: String? = null,
)

@Immutable
@Serializable
data class MomentsLabelInfo(
    @SerialName("foreground_color") val foregroundColor: MomentsFgColor? = null,
    val text: String? = null,
    val type: String? = null,
)

@Immutable
@Serializable
data class MomentsFgColor(
    val alpha: Double = 0.0,
    val group: String? = null,
)

@Immutable
@Serializable
data class MomentsLegoInfo(
    val scale: Double = 0.0,
    @SerialName("image_list") val imageList: List<MomentsLegoImage> = emptyList(),
)

@Immutable
@Serializable
data class MomentsLegoImage(
    val height: Int = 0,
    @SerialName("original_url") val originalUrl: String? = null,
    val width: Int = 0,
)

@Immutable
@Serializable
data class MomentsRelationship(
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    val voting: String? = null,
)

@Immutable
@Serializable
data class MomentsPinContent(
    val content: String? = null,
    @SerialName("fold_type") val foldType: String? = null,
    @SerialName("own_text") val ownText: String? = null,
    @SerialName("text_link_type") val textLinkType: String? = null,
    val title: String? = null,
    val type: String? = null,
    val height: Int = 0,
    @SerialName("is_gif") val isGif: Boolean = false,
    @SerialName("is_long") val isLong: Boolean = false,
    @SerialName("is_watermark") val isWatermark: Boolean = false,
    @SerialName("original_url") val originalUrl: String? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    @SerialName("watermark_url") val watermarkUrl: String? = null,
    val width: Int = 0,
)

@Immutable
@Serializable
data class MomentsPinReactionRelation(
    val vote: Int = 0,
)

@Immutable
@Serializable
data class MomentsPinVirtuals(
    @SerialName("is_favorited") val isFavorited: Boolean = false,
    @SerialName("reaction_type") val reactionType: String? = null,
)

@Immutable
@Serializable
data class MomentsRegulateInfo(
    @SerialName("is_regulating") val isRegulating: Boolean = false,
    val reason: String? = null,
)

@Immutable
@Serializable
data class MomentsReviewingInfo(
    @SerialName("is_reviewing") val isReviewing: Boolean = false,
    val reason: String? = null,
    val tips: String? = null,
)

@Immutable
@Serializable
data class MomentsExposedMedal(
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("medal_avatar_frame") val medalAvatarFrame: String? = null,
    @SerialName("medal_id") val medalId: String? = null,
)

internal object StringOrLongSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StringOrLong", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) encoder.encodeString(value) else encoder.encodeString("")
    }

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder
            ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> element.content
            else -> null
        }
    }
}
