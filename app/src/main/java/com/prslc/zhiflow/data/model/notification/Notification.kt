package com.prslc.zhiflow.data.model.notification

import com.prslc.zhiflow.data.model.Paging
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The message page: the way into each list of notifications, the cards below them, and the unread. */
@Serializable
data class ZhihuMessagePage(
    val data: List<ZhihuNotificationCard> = emptyList(),
    val head: List<ZhihuNotificationCard> = emptyList(),
    @SerialName("column_head") val columnHead: List<ZhihuNotificationCard> = emptyList(),
    val unread: ZhihuUnreadSummary = ZhihuUnreadSummary(),
    val paging: Paging = Paging(),
)

/** A page of one kind of notification. Its day separators arrive in [data] among the cards. */
@Serializable
data class ZhihuNotificationTimeline(
    val data: List<ZhihuNotificationCard> = emptyList(),
    val head: List<ZhihuNotificationCard> = emptyList(),
    val paging: Paging = Paging(),
    @SerialName("has_read") val hasRead: Boolean = false,
    val count: Int = 0,
)

/**
 * One card of a notification page.
 *
 * Three kinds share this shape and only [type] tells them apart: `entry` is a way into a list,
 * `aggregate_notification` is a notification itself, and `empty` is the separator that says which
 * day the cards under it belong to, carrying nothing but [emptyInfo].
 */
@Serializable
data class ZhihuNotificationCard(
    val id: String = "",
    @SerialName("unique_id") val uniqueId: String = "",
    val type: String = "",
    @SerialName("noti_type") val notiType: String = "",
    @SerialName("noti_subtype") val notiSubtype: String = "",
    @SerialName("card_type") val cardType: String = "",
    @SerialName("action_name") val actionName: String = "",
    val head: ZhihuNotificationHead = ZhihuNotificationHead(),
    val content: ZhihuNotificationContent = ZhihuNotificationContent(),
    @SerialName("target_source") val targetSource: ZhihuNotificationTargetSource =
        ZhihuNotificationTargetSource(),
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("unread_count") val unreadCount: Int = 0,
    val created: Long = 0,
    @SerialName("created_str") val createdStr: String = "",
    @SerialName("empty_info") val emptyInfo: ZhihuNotificationEmpty? = null,
)

@Serializable
data class ZhihuNotificationHead(
    @SerialName("avatar_urls") val avatarUrls: List<String> = emptyList(),
    @SerialName("avatar_url") val avatarUrl: String = "",
    @SerialName("target_link") val targetLink: String = "",
    val labels: List<ZhihuNotificationLabel> = emptyList(),
)

/** A mark the server puts beside whoever a notification is about — a mutual-follow note, for one. */
@Serializable
data class ZhihuNotificationLabel(
    val text: String = "",
)

/**
 * A card's wording. [subTitle] is what was done — an invitation to answer, say — and it is joined
 * to what it was done to, which the server sends as [abstractText] where it has one and [text]
 * otherwise.
 */
@Serializable
data class ZhihuNotificationContent(
    val title: String = "",
    @SerialName("sub_title") val subTitle: String = "",
    val text: String = "",
    @SerialName("abstract_text") val abstractText: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("target_link") val targetLink: String = "",
)

/**
 * The thing a notification is about, in the server's own words: [text] is the line it draws for it,
 * which is where a target that has since been deleted says so, and [subText] is its tally.
 */
@Serializable
data class ZhihuNotificationTargetSource(
    @SerialName("top_text") val topText: String = "",
    @SerialName("sub_text") val subText: String = "",
    val text: String = "",
    @SerialName("is_deleted") val isDeleted: Boolean = false,
)

@Serializable
data class ZhihuNotificationEmpty(
    val text: String = "",
    val number: Int = 0,
)

/**
 * How much is unread, counted four ways: everything, the feed, messages, and follows. Each carries
 * its own [ZhihuUnread.showCount], so a number is not always the thing to draw.
 */
@Serializable
data class ZhihuUnreadSummary(
    val total: ZhihuUnread = ZhihuUnread(),
    val feed: ZhihuUnread = ZhihuUnread(),
    val message: ZhihuUnread = ZhihuUnread(),
    @SerialName("follow_updates") val followUpdates: ZhihuUnread = ZhihuUnread(),
    @SerialName("can_message_read_all") val canMessageReadAll: Boolean = false,
    @SerialName("new_message_text") val newMessageText: String = "",
)

@Serializable
data class ZhihuUnread(
    val count: Int = 0,
    @SerialName("show_count") val showCount: Boolean = false,
)
