package com.prslc.zhiflow.data.repository

import com.prslc.zhiflow.core.exception.mapOffMain
import com.prslc.zhiflow.data.dto.NotificationCategoryDto
import com.prslc.zhiflow.data.dto.NotificationItemDto
import com.prslc.zhiflow.data.dto.UnreadSummaryDto
import com.prslc.zhiflow.data.mapper.toCategories
import com.prslc.zhiflow.data.mapper.toDto
import com.prslc.zhiflow.data.mapper.toItems
import com.prslc.zhiflow.data.remote.service.NotificationService

/** The message page: what it offers a way into, and how much of it is unread. */
data class MessagePageResult(
    val categories: List<NotificationCategoryDto>,
    val unread: UnreadSummaryDto,
)

/** One page of a list of notifications. */
data class NotificationListResult(
    val items: List<NotificationItemDto>,
    val nextPageUrl: String?,
)

class NotificationRepository(private val service: NotificationService) {

    /**
     * Retrieves the message page.
     *
     * @return A [Result] holding the entries into each list and the unread counts.
     */
    suspend fun messagePage(): Result<MessagePageResult> = service.messagePage()
        .mapOffMain { page ->
            MessagePageResult(
                categories = page.toCategories(),
                unread = page.unread.toDto(),
            )
        }

    /**
     * Retrieves a page of one kind of notification.
     *
     * @param type Which list to read, in the spelling the message page names it by.
     * @param nextUrl The page to ask for, from a previous result's cursor; null asks for the first.
     * @return A [Result] holding the rows and the cursor for the page after them when there is one.
     */
    suspend fun notifications(
        type: String,
        nextUrl: String? = null,
    ): Result<NotificationListResult> = service.timeline(type, nextUrl)
        .mapOffMain { timeline ->
            NotificationListResult(
                items = timeline.toItems(),
                nextPageUrl = timeline.paging.next.takeIf { it.isNotBlank() },
            )
        }

    /**
     * Marks every notification of one kind as read.
     *
     * @param type Which list, as [notifications] takes it.
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun markAllRead(type: String): Result<Unit> = service.markAllRead(type)

    /**
     * Reports that the reader has opened the message tab.
     *
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun reportTabAccess(): Result<Unit> = service.reportTabAccess()
}
