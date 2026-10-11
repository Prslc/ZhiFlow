package com.prslc.zhiflow.data.remote.service

import com.prslc.zhiflow.core.network.ApiClient
import com.prslc.zhiflow.core.network.BASE_URL
import com.prslc.zhiflow.core.network.safeApiCall
import com.prslc.zhiflow.core.network.safeExecute
import com.prslc.zhiflow.data.model.notification.ZhihuMessagePage
import com.prslc.zhiflow.data.model.notification.ZhihuNotificationTimeline
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Service handling notifications: the message page, the lists it opens, and what has been read.
 */
class NotificationService(private val api: ApiClient) {

    /**
     * Retrieves the message page: the way into each list, the cards under them, and the unread.
     *
     * @param nextUrl The page of cards to ask for, from `paging.next` of a previous response. That
     *   url already carries both cursors the page takes, so nothing is added to it; null asks for a
     *   first page, whose parameters are set here.
     * @return A [Result] holding the page, or the failure that kept it away.
     */
    suspend fun messagePage(nextUrl: String? = null): Result<ZhihuMessagePage> = api.safeApiCall {
        val url = nextUrl?.toHttpUrl()
            ?: "${BASE_URL}/notifications/v3/message/v3".toHttpUrl().newBuilder()
                .addQueryParameter("limit", MESSAGE_PAGE_SIZE.toString())
                .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }

    /**
     * Retrieves a page of one kind of notification.
     *
     * @param type Which list: `comment`, `like`, `follow`, and the rest the message page names.
     * @param nextUrl The page to ask for, from `paging.next` of a previous response. That url
     *   already carries the kind and the cursor, so nothing is added to it; null asks for a first
     *   page, whose parameters are set here.
     * @return A [Result] holding the page of cards, or the failure that kept them away.
     */
    suspend fun timeline(
        type: String,
        nextUrl: String? = null,
    ): Result<ZhihuNotificationTimeline> = api.safeApiCall {
        val url = nextUrl?.toHttpUrl()
            ?: "${BASE_URL}/notifications/v3/timeline/entry/$type".toHttpUrl().newBuilder()
                .addQueryParameter("limit", PAGE_SIZE.toString())
                // Only invitations are cut into days; the other lists take nothing for it.
                .apply { if (type == INVITE_TYPE) addQueryParameter("invite_with_time_slice", "1") }
                .build()

        Request.Builder()
            .url(url)
            .get()
            .build()
    }

    /**
     * Marks every notification of one kind as read.
     *
     * The server answers with no body, so only the outcome is read.
     *
     * @param type Which list, as [timeline] takes it.
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun markAllRead(type: String): Result<Unit> = api.safeExecute {
        Request.Builder()
            .url("${BASE_URL}/notifications/v3/timeline/entry/$type/actions/readall")
            .post(EMPTY_BODY)
            .build()
    }

    /**
     * Reports that the reader has opened the message tab, which is how the server counts a visit.
     *
     * @return A [Result] carrying the failure when the server refused.
     */
    suspend fun reportTabAccess(): Result<Unit> = api.safeExecute {
        Request.Builder()
            .url("${BASE_URL}/notifications/v3/message/tab_access")
            .post(EMPTY_BODY)
            .build()
    }

    private companion object {
        /** The message page is a handful of conversations, and asks for fewer than a list does. */
        const val MESSAGE_PAGE_SIZE = 10
        const val PAGE_SIZE = 20
        const val INVITE_TYPE = "invite"
        val EMPTY_BODY: RequestBody = "".toRequestBody(null)
    }
}
