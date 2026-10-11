package com.prslc.zhiflow.data.mapper

import com.prslc.zhiflow.data.dto.MessageThreadDto
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.data.dto.NotificationItemDto
import com.prslc.zhiflow.data.dto.UnreadCountDto
import com.prslc.zhiflow.data.dto.UnreadSummaryDto
import com.prslc.zhiflow.data.model.notification.ZhihuMessagePage
import com.prslc.zhiflow.data.model.notification.ZhihuNotificationCard
import com.prslc.zhiflow.data.model.notification.ZhihuNotificationEmpty
import com.prslc.zhiflow.data.model.notification.ZhihuNotificationTimeline
import com.prslc.zhiflow.data.model.notification.ZhihuUnread
import com.prslc.zhiflow.data.model.notification.ZhihuUnreadSummary
import com.prslc.zhiflow.data.remote.parser.htmlToAnnotatedString
import com.prslc.zhiflow.data.remote.parser.htmlToPlainText

/** The lists the message page offers a way into, in the spelling `timeline/entry/{type}` takes. */
private val CATEGORY_TYPES = setOf(
    "invite",
    "system",
    "system_message",
    "comment",
    "like",
    "favlist_me",
    "follow",
)

/** What an entry card writes in front of the identifier it carries. */
private const val ENTRY_PREFIX = "entry_"

/** The card kinds this app knows: two it draws, and the separator that is only a day. */
private const val EMPTY_TYPE = "empty"
private const val ENTRY_TYPE = "entry"
private const val AGGREGATE_TYPE = "aggregate_notification"

/** The kind of card that carries a conversation rather than a notification. */
private const val THREAD_TYPE = "message"

/** The stretch of a conversation card's link that the other end's user hash follows. */
private const val INBOX_SEGMENT = "/inbox"

/** The parameter of a conversation card's link that says how the page was opened. */
private const val SOURCE_PARAM = "source_type"

/**
 * The ways into each list of notifications.
 *
 * An entry this app cannot name a list for is dropped rather than guessed at: the identifier is what
 * opens that list, and a row that opened nothing would be worse than no row.
 *
 * @return The entries, in the order the server put them.
 */
internal fun ZhihuMessagePage.toCategories(): List<NotificationCategoryDto> =
    head.mapNotNull { card ->
        val type = card.categoryType() ?: return@mapNotNull null
        NotificationCategoryDto(
            type = type,
            title = htmlToPlainText(card.content.title),
            text = htmlToPlainText(card.content.text),
            unreadCount = card.unreadCount,
        )
    }

/**
 * The conversations the message page lists.
 *
 * The page mixes them with cards of other kinds, and every one of them is drawn the same way; what
 * separates a conversation is that it names the person it is with.
 *
 * @return The rows, in the order the server put them.
 */
internal fun ZhihuMessagePage.toThreads(): List<MessageThreadDto> = data.mapNotNull { card ->
    val title = htmlToPlainText(card.content.title)
    val text = htmlToPlainText(card.content.text)
    if (title.isBlank() && text.isBlank()) return@mapNotNull null

    MessageThreadDto(
        id = card.id,
        peerId = card.peerId(),
        sourceType = card.sourceType(),
        title = title,
        text = text,
        avatarUrl = card.avatarUrl(),
        unreadCount = card.unreadCount,
        createdTime = card.created,
        targetLink = card.link(),
    )
}

/**
 * The cards of one list of notifications, with the day separators left where the server put them.
 *
 * @return The rows in the order the server sent them.
 */
internal fun ZhihuNotificationTimeline.toItems(): List<NotificationItemDto> = data.mapNotNull { card ->
    when (card.type) {
        EMPTY_TYPE -> card.emptyInfo?.toDay()
        ENTRY_TYPE, AGGREGATE_TYPE -> card.toEntry()
        else -> null
    }
}

/**
 * How much is unread, as the badge and the lists need it.
 *
 * @return The four counts, each carrying whether the server wants its number drawn.
 */
internal fun ZhihuUnreadSummary.toDto(): UnreadSummaryDto = UnreadSummaryDto(
    total = total.toDto(),
    feed = feed.toDto(),
    message = message.toDto(),
    followUpdates = followUpdates.toDto(),
)

private fun ZhihuUnread.toDto(): UnreadCountDto = UnreadCountDto(count = count, showCount = showCount)

/** @return The separator as the row that names a day, or null when it names none. */
private fun ZhihuNotificationEmpty.toDay(): NotificationItemDto.Day? =
    text.takeIf { it.isNotBlank() }?.let { NotificationItemDto.Day(text = it, count = number) }

/** @return The card as a notification row, or null when it carries nothing to draw. */
private fun ZhihuNotificationCard.toEntry(): NotificationItemDto.Entry? {
    val title = htmlToPlainText(content.title)
    val subTitle = htmlToPlainText(content.subTitle)
    // The body is the abstract where the server sent one, and the text otherwise. It is the one
    // part of a card drawn as the markup it arrives in: the links in it are meant to be followed,
    // and the emoji in it are pictures.
    val body = content.abstractText.ifBlank { content.text }
    val text = htmlToAnnotatedString(body)
    val targetText = htmlToPlainText(targetSource.text)
    if (title.isBlank() && subTitle.isBlank() && body.isBlank() && targetText.isBlank()) return null

    return NotificationItemDto.Entry(
        id = id,
        title = title,
        subTitle = subTitle,
        text = text,
        targetText = targetText,
        isContentDeleted = content.isDeleted,
        label = head.labels.firstOrNull { it.text.isNotBlank() }
            ?.let { htmlToPlainText(it.text) },
        avatarUrl = avatarUrl(),
        unreadCount = unreadCount,
        isRead = isRead,
        createdTime = created,
        targetLink = link(),
    )
}

/**
 * The list an entry card opens, or null when it names none this app knows.
 *
 * Where a real card carries the identifier is not settled — the samples give it both as the tail of
 * the card's link and as the subtype with an `entry_` in front — so both are read.
 */
private fun ZhihuNotificationCard.categoryType(): String? {
    val subtype = notiSubtype.removePrefix(ENTRY_PREFIX)
    if (subtype in CATEGORY_TYPES) return subtype

    val fromContent = linkTail(content.targetLink)
    if (fromContent in CATEGORY_TYPES) return fromContent

    return linkTail(head.targetLink).takeIf { it in CATEGORY_TYPES }
}

/**
 * The person a conversation is with, which the address the card opens names: it is
 * `.../inbox/{user hash}`, with the title and the source after it.
 *
 * @return The hash id, or null for a card that is not a conversation this app can open.
 */
private fun ZhihuNotificationCard.peerId(): String? {
    if (notiType != THREAD_TYPE) return null

    val path = link().substringBefore('?')
    val start = path.indexOf("$INBOX_SEGMENT/")
    if (start < 0) return null

    val id = path.substring(start + INBOX_SEGMENT.length + 1).substringBefore('/')
    return id.takeIf { it.isNotBlank() }
}

/**
 * How a conversation card's address says the page is opened, which is the server's to be told again
 * when the reader writes: the value is the link's, not one this app works out.
 *
 * @return The source the link names, or null for a card that names none.
 */
private fun ZhihuNotificationCard.sourceType(): String? =
    link().substringAfter('?', "")
        .takeIf { it.isNotEmpty() }
        ?.split('&')
        ?.firstOrNull { it.startsWith("$SOURCE_PARAM=") }
        ?.substringAfter('=')

/** The picture a card is drawn with: the one it carries, or the first of the list it carries. */
private fun ZhihuNotificationCard.avatarUrl(): String? =
    head.avatarUrl.ifBlank { head.avatarUrls.firstOrNull().orEmpty() }.takeIf { it.isNotBlank() }

/** @return Where the card points, preferring what it says over what its head says. */
private fun ZhihuNotificationCard.link(): String = content.targetLink.ifBlank { head.targetLink }

/** @return The last stretch of a link's path, which is where an identifier sits. */
private fun linkTail(url: String): String =
    url.substringBefore('?').trimEnd('/').substringAfterLast('/')
