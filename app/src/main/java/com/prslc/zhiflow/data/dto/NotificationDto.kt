package com.prslc.zhiflow.data.dto

import androidx.compose.runtime.Immutable

/** One way into a list of notifications: what the entry says, and which list it opens. */
@Immutable
data class NotificationCategoryDto(
    val type: String,
    val title: String,
    val text: String,
    val unreadCount: Int,
)

/**
 * One row of the message page: a conversation, or a message the server put beside the others.
 *
 * [peerId] is null for a card that opens something other than a conversation, which is then
 * [targetLink]'s to say; [sourceType] is how that address says the page is opened.
 */
@Immutable
data class MessageThreadDto(
    val id: String,
    val peerId: String?,
    val sourceType: String?,
    val title: String,
    val text: String,
    val avatarUrl: String?,
    val unreadCount: Int,
    val createdTime: Long,
    val targetLink: String,
)

/**
 * One row of a list of notifications: a card, or the separator naming the day it belongs to.
 *
 * A card is drawn in layers, each of which the server may leave empty: who it is about
 * ([Entry.title]), what they did and what it was done to, which are drawn as one line
 * ([Entry.subTitle] and [Entry.text]), and the server's own line for that thing ([Entry.targetText]).
 */
@Immutable
sealed interface NotificationItemDto {

    /** The day the cards under it belong to, and how many of them it holds. */
    @Immutable
    data class Day(val text: String, val count: Int) : NotificationItemDto

    @Immutable
    data class Entry(
        val id: String,
        val title: String,
        val subTitle: String,
        val text: String,
        val targetText: String,
        val isContentDeleted: Boolean,
        val label: String?,
        val avatarUrl: String?,
        val unreadCount: Int,
        val isRead: Boolean,
        val createdTime: Long,
        val targetLink: String,
    ) : NotificationItemDto
}

/**
 * How much is unread, counted the four ways the server counts it.
 *
 * All four are carried because the message tab's own share of them is not settled: what the badge
 * draws today is [total], and the rest are what it would draw instead.
 */
@Immutable
data class UnreadSummaryDto(
    val total: UnreadCountDto,
    val feed: UnreadCountDto,
    val message: UnreadCountDto,
    val followUpdates: UnreadCountDto,
)

/** One of those counts, with whether the number is the thing to draw. */
@Immutable
data class UnreadCountDto(
    val count: Int,
    val showCount: Boolean,
)
